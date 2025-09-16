package vn.neosoft;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpSession;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class SQL {
    private static final String jdbcURL = "jdbc:mysql://sql12.freesqldatabase.com/sql12796558";
    private static final String username = "sql12796558";
    private static final String password = "wfy1yg9E9a";
    private final function methodFunction = new function();

    public void resetDatabase() {
        String dropNotifications = "DROP TABLE IF EXISTS notifications;";
        String dropBorrowings   = "DROP TABLE IF EXISTS borrowings;";
        String dropBooks        = "DROP TABLE IF EXISTS books;";
        String dropUsers        = "DROP TABLE IF EXISTS tv_user;";

        String createUsers = """
            CREATE TABLE tv_user (
                username VARCHAR(50) PRIMARY KEY,
                password VARCHAR(255) NOT NULL,
                name VARCHAR(100) NOT NULL,
                role VARCHAR(15) NULL,
                phone VARCHAR(15),
                status VARCHAR(20),
                points INT
            );
            """;

        String createBooks = """
            CREATE TABLE books (
                book_id INT PRIMARY KEY AUTO_INCREMENT,
                title VARCHAR(255) NOT NULL,
                author VARCHAR(100) NOT NULL,
                description TEXT,
                quantity INT NOT NULL DEFAULT 0,
                image VARCHAR(255),
                category VARCHAR(255),
                viewers INT,
                exchange_point INT,
                borrowing_count INT
            );
            """;

        String createBorrowings = """
            CREATE TABLE borrowings (
                id INT PRIMARY KEY AUTO_INCREMENT,
                username VARCHAR(50),
                book_id INT,
                borrow_date TEXT NOT NULL,
                due_date TEXT NOT NULL,
                return_date TEXT,
                FOREIGN KEY (username) REFERENCES tv_user(username) ON DELETE CASCADE,
                FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE
            );
            """;

        String createNotifications = """
            CREATE TABLE notifications (
                id INT PRIMARY KEY AUTO_INCREMENT,
                message TEXT NOT NULL,
                username VARCHAR(50) NULL,         -- NULL = thông báo cho tất cả
                is_read TINYINT(1) NOT NULL DEFAULT 0,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
            """;

        try (Connection connection = DriverManager.getConnection(jdbcURL, username, password);
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(dropNotifications);
            statement.executeUpdate(dropBorrowings);
            statement.executeUpdate(dropBooks);
            statement.executeUpdate(dropUsers);

            System.out.println("Dropped existing tables successfully!");

            statement.executeUpdate(createUsers);
            statement.executeUpdate(createBooks);
            statement.executeUpdate(createBorrowings);
            statement.executeUpdate(createNotifications);

            System.out.println("Created tables successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    // 200 thành công
    // 404 lỗi xử lý
    // 505 lỗi sql
    //          <--------------------MANAGE SIGNIN/SIGNOUT--------------------->
    public int registerUser(String user,String pass, String name, String role, String phone) {
        String sql = "INSERT INTO tv_user (username,password,name,role,phone,status,points) VALUES (?,?,?,?,?,?,?)";
        try
        {
            Connection connection = DriverManager.getConnection(jdbcURL, username, password);
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, user);
            statement.setString(2, pass);
            statement.setString(3, name);
            statement.setString(4, role);
            statement.setString(5, phone);
            statement.setString(6,"false");
            statement.setInt(7, 0);
            statement.executeUpdate();
            statement.close();
            connection.close();
            return 200;

        } catch (SQLException e) {
            methodFunction.logException(e);
            return 404;
        }
    }


    public String loginUser(String user, String pass, HttpSession session) {
        String sql = "SELECT username, password, name, role, phone,status,points FROM tv_user WHERE username = ?";
        String json ="";
        try {
            Connection connection = DriverManager.getConnection(jdbcURL, username, password);
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, user);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String username = resultSet.getString("username");
                String hashedPassword = resultSet.getString("password");
                String name = resultSet.getString("name");
                String role = resultSet.getString("role");
                String phone = resultSet.getString("phone");
                String status = resultSet.getString("status");
                int points = resultSet.getInt("points");
                if(status.equals("true"))
                {
                    return "403";
                }
                if(role.equalsIgnoreCase("user")) {
                    if (pass.equals(hashedPassword)) {
                        User people = new User(username, pass, name, role, phone,points);
                        Gson gson = new Gson();
                        json = gson.toJson(people);
                        session.setAttribute("username", username);
                        session.setAttribute("name", name);
                        session.setAttribute("role", role);
                        session.setMaxInactiveInterval(300); // 5 phút
                        handlePoliciesOnLogin(username);
                    }
                }
                else if(role.equalsIgnoreCase("admin"))
                {
                    if (pass.equals(hashedPassword)) {

                        User people = new User(username, pass, name, role, phone, points);
                        Gson gson = new Gson();
                        json = gson.toJson(people);
                        session.setAttribute("username", username);
                        session.setAttribute("name", name);
                        session.setAttribute("role", role);
                        session.setMaxInactiveInterval(300); // 5 phút

                    }
                }
            }

            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            methodFunction.logException(e);
            return "404";
        }
        System.out.println(json);
        return json;
    }

    //          <--------------------MANAGE BOOKS--------------------->

    // Khóa tài khoản tự động và gửi cảnh báo

    private void handlePoliciesOnLogin(String username) {
        try (Connection conn = DriverManager.getConnection(jdbcURL, username, password)) {
            LocalDate today = LocalDate.now();

            // 1) THÔNG BÁO SẮP HẾT HẠN (<= 3 ngày, chưa trả)
            String dueSoonSql = """
            SELECT b.title, br.due_date
            FROM borrowings br
            JOIN books b ON b.book_id = br.book_id
            WHERE br.username = ?
              AND br.return_date IS NULL
        """;
            ArrayList<String> dueSoonItems = new ArrayList<>();

            try (PreparedStatement ps = conn.prepareStatement(dueSoonSql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String title = rs.getString("title");
                        String dueStr = rs.getString("due_date"); // TEXT yyyy-MM-dd
                        if (dueStr == null || dueStr.isBlank()) continue;

                        LocalDate due = LocalDate.parse(dueStr); // parse TEXT
                        // nếu còn hạn và khoảng cách <=3 ngày
                        if (!today.isAfter(due)) {
                            long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, due);
                            if (daysLeft <= 3) {
                                dueSoonItems.add(title + " (hết hạn: " + due + ")");
                            }
                        }
                    }
                }
            }
            if (!dueSoonItems.isEmpty()) {
                sendNotificationToUser(
                        "Các sách sắp đến hạn (≤ 3 ngày): " + String.join(" | ", dueSoonItems),
                        username
                );
            }

            // 2) QUÁ HẠN CHƯA TRẢ → TRỪ ĐIỂM & KHÓA TÀI KHOẢN
            //    (tránh trừ lặp: chỉ trừ khi tài khoản hiện đang mở 'false')
            String findOverdueSql = """
            SELECT b.title, br.due_date
            FROM borrowings br
            JOIN books b ON b.book_id = br.book_id
            WHERE br.username = ?
              AND br.return_date IS NULL
        """;

            boolean hasOverdue = false;
            ArrayList<String> overdueTitles = new ArrayList<>();

            try (PreparedStatement ps = conn.prepareStatement(findOverdueSql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String title = rs.getString("title");
                        String dueStr = rs.getString("due_date");
                        if (dueStr == null || dueStr.isBlank()) continue;

                        LocalDate due = LocalDate.parse(dueStr);
                        if (today.isAfter(due)) { // đã quá hạn
                            hasOverdue = true;
                            overdueTitles.add(title + " (quá hạn: " + due + ")");
                        }
                    }
                }
            }

            if (hasOverdue) {
                // trạng thái & điểm hiện tại
                String getUserSql = "SELECT status, points FROM tv_user WHERE username = ?";
                String status = "false";
                int points = 0;
                try (PreparedStatement ps = conn.prepareStatement(getUserSql)) {
                    ps.setString(1, username);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            status = rs.getString("status");
                            points = rs.getInt("points");
                        }
                    }
                }

                // chỉ xử lý nếu hiện đang mở (tránh trừ lặp khi đã khóa)
                if (!"true".equalsIgnoreCase(status)) {
                    // trừ tối đa 100 điểm một lần (nếu đủ)
                    int deducted = Math.min(points, 100);
                    if (deducted > 0) {
                        String deductSql = "UPDATE tv_user SET points = points - ? WHERE username = ?";
                        try (PreparedStatement ps = conn.prepareStatement(deductSql)) {
                            ps.setInt(1, deducted);
                            ps.setString(2, username);
                            ps.executeUpdate();
                        }
                    }

                    // khóa tài khoản
                    String lockSql = "UPDATE tv_user SET status = 'true' WHERE username = ?";
                    try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                        ps.setString(1, username);
                        ps.executeUpdate();
                    }

                    // thông báo
                    String msg = "Tài khoản đã bị KHÓA do có sách quá hạn chưa trả: "
                            + String.join(" | ", overdueTitles);
                    if (deducted > 0) {
                        msg += ". Đã trừ " + deducted + " điểm tích lũy.";
                    } else {
                        msg += ". Không trừ điểm (điểm hiện tại = 0).";
                    }
                    sendNotificationToUser(msg, username);
                }
            }

        } catch (SQLException ex) {
            methodFunction.logException(ex);
        } catch (Exception ex) { // lỗi parse ngày...
            methodFunction.logException(ex);
        }
    }


    // Khóa tài khoản thủ công
    public int lockUser(String user) {
        // SQL query để cập nhật trạng thái khóa tài khoản
        String sql = "UPDATE tv_user SET status = 'true' WHERE username = ?";

        try {
            Connection connection = DriverManager.getConnection(jdbcURL, username, password);
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, user);

            int rowsAffected = statement.executeUpdate();

            statement.close();
            connection.close();

            if (rowsAffected > 0) {
                sendNotificationToAll("Your account has been locked by administrator");
                return 200;
            } else {
                return 404;
            }

        } catch (SQLException e) {
            methodFunction.logException(e);
            return 505;
        }
    }
    // Mở tài khoản
    public int unlockUser(String user) {
        String sql = "UPDATE tv_user SET status = 'false' WHERE username = ?";
        try {
            Connection connection = DriverManager.getConnection(jdbcURL, username, password);
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, user);
            int rowsAffected = statement.executeUpdate();
            statement.close();
            connection.close();
            if (rowsAffected > 0) {
                sendNotificationToAll("Your account has been unlocked by administrator");
                return 200;
            } else {
                return 404;
            }

        } catch (SQLException e) {
            methodFunction.logException(e);
            return 505;
        }
    }
    public String checkAccountStatus(String user) {
        String query = "SELECT status FROM tv_user WHERE username = ?";
        try {
            Connection connection = DriverManager.getConnection(jdbcURL, username, password);
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, user);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("status");
            }
        } catch (SQLException e) {
            methodFunction.logException(e);
        }
        return null;
    }

    // Thêm sách
    public int addBook(String title, String author,String description, int quantity, String image, String category ) {
        try {
            Connection connection = DriverManager.getConnection(jdbcURL, username, password);
            String query = "INSERT INTO books (title, author, description,quantity,image,category,viewers,exchange_point,borrowing_count) VALUES (?,?,?,?,?,?,?,?,?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, title);
            statement.setString(2, author);
            statement.setString(3, description);
            statement.setInt(4, quantity);
            statement.setString(5, image);
            statement.setString(6, category);
            statement.setInt(7,0);
            statement.setInt(8,0);
            statement.setInt(9,0);
            int check = statement.executeUpdate();
            statement.close();
            connection.close();
            if(check > 0) {
                sendNotificationToAll("New  " + title + " books have just been added to the library.");
                return 200;
            }
            else
                return 404;
        } catch (Exception e) {
            methodFunction.logException(e);
            return 404;
        }
    }

    public int addBooks(String keyword,String quantity_res) {
        try {
            String encodedKeyword = URLEncoder.encode(keyword, "UTF-8");
            String apiUrl = "https://www.googleapis.com/books/v1/volumes?q="+encodedKeyword+"&maxResults="+quantity_res;
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            // Phân tích chuỗi JSON trả về
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray items = jsonResponse.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                JSONObject bookJson = items.getJSONObject(i).getJSONObject("volumeInfo");
                String title = bookJson.getString("title");
                String author = bookJson.has("authors") ? bookJson.getJSONArray("authors").getString(0) : "Unknown Author";
                String description = bookJson.has("description") ? bookJson.getString("description") : "No Description";
                int quantity = 5;
                String image = bookJson.has("imageLinks") ? bookJson.getJSONObject("imageLinks").getString("thumbnail") : "";
                String category = bookJson.has("categories") ? bookJson.getJSONArray("categories").getString(0) : "Unknown Category";
                int result = addBook(title, author, description, quantity, image, category);
                if (result != 200) {
                    return 404;
                }
            }
            return 200;
        } catch (Exception e) {
           methodFunction.logException(e);
            return 404;
        }
    }
    // Xóa sách
    public int deleteBook(String title) {
        try {
            Connection connection = DriverManager.getConnection(jdbcURL, username, password);
            String query;
            PreparedStatement statement;
            query = "DELETE FROM books WHERE title = ?";
            statement = connection.prepareStatement(query);
            statement.setString(1, title);
            statement.executeUpdate();
            statement.close();
            connection.close();
            return 200;
        } catch (Exception e) {
            methodFunction.logException(e);
            return 404;
        }
    }

    // Chỉnh sửa sách bằng ID hoặc Tên
    public int editBook(String title, String newTitle, String newAuthor, String newDes, int newQuantity, String newImage) {
        try {
            Connection connection = DriverManager.getConnection(jdbcURL, username, password);
            String query = "UPDATE books SET title = ?, author = ?,description = ?, quantity = ?, image = ? WHERE title = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(6, title);
            statement.setString(1, newTitle);
            statement.setString(2, newAuthor);
            statement.setString(3, newDes);
            statement.setInt(4, newQuantity);
            statement.setString(5, newImage);
            statement.executeUpdate();
            statement.close();
            connection.close();
            return 200;
        } catch (Exception e) {
            methodFunction.logException(e);
            return 404;
        }
    }

    public String getAllBooks() {
        ArrayList<Books> books = new ArrayList<>();
        Gson gson = new Gson().newBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        try {
            Connection connection = DriverManager.getConnection(jdbcURL, username, password);
            String query = "SELECT * FROM books";
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("book_id");
                String title = resultSet.getString("title");
                String author = resultSet.getString("author");
                String description = resultSet.getString("description");
                int quantity = resultSet.getInt("quantity");
                String image = resultSet.getString("image");
                String category = resultSet.getString("category");
                int viewers = resultSet.getInt("viewers");
                int exchange_point = resultSet.getInt("exchange_point");
                int borrowing_count = resultSet.getInt("borrowing_count");
                books.add(new Books(id,title, author, description, quantity, image,category,viewers
                        ,exchange_point,borrowing_count));
            }
            resultSet.close();
            statement.close();
            connection.close();
        } catch (Exception e) {
            methodFunction.logException(e);
            return "404";
        }
        return gson.toJson(books);
    }
    public String getBorrowingHistory() {
        String query = "SELECT * FROM borrowings";
        ArrayList<BorrowingRecord> borrowingsList = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(jdbcURL, username, password);
             PreparedStatement statement = connection.prepareStatement(query)) {

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    BorrowingRecord record = new BorrowingRecord();
                    record.setId(resultSet.getInt("id"));
                    record.setUsername(resultSet.getString("username"));
                    record.setBookId(resultSet.getInt("book_id"));
                    record.setBorrowDate(resultSet.getDate("borrow_date").toString());
                    record.setDueDate(resultSet.getDate("due_date").toString());
                    Date returnDate = resultSet.getDate("return_date");
                    record.setReturnDate(returnDate != null ? returnDate.toString() : null);

                    borrowingsList.add(record);
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            methodFunction.logException(e);
            return "404";
        }
        Gson gson = new Gson().newBuilder().setPrettyPrinting().create();
        return gson.toJson(borrowingsList);
    }
    public String getInfo(String user)
    {
        ArrayList<User>userList =new ArrayList<>();
        String query = "SELECT * FROM tv_user WHERE username = ?";
        try {
            Connection connection = DriverManager.getConnection(jdbcURL, username, password);
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1,user);
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next())
            {
                String username = resultSet.getString("username");
                String pass = resultSet.getString("password");
                String name = resultSet.getString("name");
                String role = resultSet.getString("role");
                String phone = resultSet.getString("phone");
                String status = resultSet.getString("status");
                int points = resultSet.getInt("points");
                User user1 = new User(username, pass, name, role, phone,status,points);
                userList.add(user1);
            }
            resultSet.close();
            preparedStatement.close();
            connection.close();
        }
        catch (Exception e)
        {
            methodFunction.logException(e);
            return "404";
        }
        Gson gson = new Gson().newBuilder().setPrettyPrinting().create();
        return gson.toJson(userList);
    }
    public String getUser() {
        String query = "SELECT * FROM tv_user WHERE role = 'user'";
        ArrayList<User> userList = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(jdbcURL, username, password);
             PreparedStatement statement = connection.prepareStatement(query)) {

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String username = resultSet.getString("username");
                    String pass = resultSet.getString("password");
                    String name = resultSet.getString("name");
                    String role = resultSet.getString("role");
                    String phone = resultSet.getString("phone");
                    String status = resultSet.getString("status");
                    int points = resultSet.getInt("points");
                    User user = new User(username, pass, name, role, phone,status,points);
                    userList.add(user);
                }
            }
        } catch (SQLException e) {

            methodFunction.logException(e);
            return "500";
        }

        Gson gson = new Gson().newBuilder().setPrettyPrinting().create();
        return gson.toJson(userList);
    }
    // Cập nhật view sách dựa vào title
    public int updateView(String title, boolean increment) {
        try {
            Connection connection = DriverManager.getConnection(jdbcURL, username, password);
            String query1 = "SELECT viewers FROM books WHERE title = ?";
            PreparedStatement statement1 = connection.prepareStatement(query1);
            statement1.setString(1, title);
            ResultSet resultSet = statement1.executeQuery();

            if (resultSet.next()) {
                int viewers = resultSet.getInt("viewers");
                if (increment) {
                    viewers += 1;
                } else {
                    viewers = Math.max(0, viewers - 1);
                }
                String query2 = "UPDATE books SET viewers = ? WHERE title = ?";
                PreparedStatement statement2 = connection.prepareStatement(query2);
                statement2.setInt(1, viewers);
                statement2.setString(2, title);
                statement2.executeUpdate();
                statement1.close();
                statement2.close();
                connection.close();
                return 200;
            } else {
                methodFunction.log("updateView - Khong tim thay sach");
                return 404;
            }
        } catch (Exception e) {
            methodFunction.logException(e);
            return 404;
        }
    }
    // Lấy các thể loại
    public String getAllCategories() {
        ArrayList<String> categories = new ArrayList<>();
        String query = "SELECT DISTINCT category FROM books";

        try (Connection connection = DriverManager.getConnection(jdbcURL, username, password);
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                categories.add(rs.getString("category"));
            }

        } catch (SQLException e) {
           methodFunction.logException(e);
           return "404";
        }
        Gson gson = new Gson().newBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        return gson.toJson(categories);
    }
    // Hiển thị sách dựa vào the loai
    public ArrayList<Books> getBooksByCategory(String category) {
        ArrayList<Books> books = new ArrayList<>();
        String query = "SELECT * FROM books WHERE category = ?";

        try (Connection connection = DriverManager.getConnection(jdbcURL, username, password);
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Books book = new Books(
                        rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("description"),
                        rs.getInt("quantity"),
                        rs.getString("image"),
                        rs.getString("category"),
                        rs.getInt("viewers"),
                        rs.getInt("exchange_point"),
                        rs.getInt("borrowing_count")
                );
                books.add(book);
            }

        } catch (SQLException e) {
           methodFunction.logException(e);
        }

        return books;
    }

    //                      <------------------USER----------------->

// Chức năng mượn sách
    //100 sách đã mượn chưa trả
    //101 hết sách
    // 200 thành công
    // 404 not found
    //103 khong du diem
    //500 loi server
    // Chức năng mượn sách
    public String borrowBook(String usernames, int bookId) {
        try (Connection connection = DriverManager.getConnection(jdbcURL, username, password)) {
            // Kiểm tra xem người dùng đã mượn sách này mà chưa trả hay chưa
            String checkBorrowingQuery = "SELECT * FROM borrowings WHERE username = ? AND book_id = ? AND return_date IS NULL";
            try (PreparedStatement checkBorrowingStmt = connection.prepareStatement(checkBorrowingQuery)) {
                checkBorrowingStmt.setString(1, usernames);
                checkBorrowingStmt.setInt(2, bookId);
                ResultSet rs = checkBorrowingStmt.executeQuery();

                if (rs.next()) {

                    return "100";
                }
            }

            // Kiểm tra số lượng sách có sẵn
            String checkQuantityQuery = "SELECT quantity FROM books WHERE book_id = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuantityQuery)) {
                checkStmt.setInt(1, bookId);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    int quantity = rs.getInt("quantity");
                    if (quantity < 1) {
                        return "101";
                    }
                } else {

                    return "404";
                }
            }

            // Cập nhật số lượng sách
            String updateQuantityQuery = "UPDATE books SET quantity = quantity - 1, borrowing_count = borrowing_count +1 WHERE book_id = ?";
            try (PreparedStatement updateStmt = connection.prepareStatement(updateQuantityQuery)) {
                updateStmt.setInt(1, bookId);
                updateStmt.executeUpdate();
            }

            // Thêm bản ghi mượn sách vào bảng borrowings
            String borrowBookQuery = "INSERT INTO borrowings (username, book_id, borrow_date, due_date, return_date) " +
                    "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement borrowStmt = connection.prepareStatement(borrowBookQuery)) {
                LocalDate borrowDate = LocalDate.now();
                LocalDate dueDate = borrowDate.plusWeeks(1);

                borrowStmt.setString(1, usernames);
                borrowStmt.setInt(2, bookId);
                borrowStmt.setDate(3, Date.valueOf(borrowDate));
                borrowStmt.setDate(4, Date.valueOf(dueDate));
                borrowStmt.setNull(5, Types.DATE); // return_date là null khi sách chưa được trả
                borrowStmt.executeUpdate();
            }

            String updatepoint = "UPDATE tv_user SET points = points + 10 WHERE username = ?";
            try (PreparedStatement updatp = connection.prepareStatement(updatepoint)) {
                updatp.setString(1, usernames);
                updatp.executeUpdate();
            }

            return "200";
        } catch (SQLException e) {
            methodFunction.logException(e);
            return "404";
        }
    }
    // tra sach
    public String returnBook(String usernames, int bookId) {
        try (Connection connection = DriverManager.getConnection(jdbcURL, username, password)) {
            // Kiểm tra bản ghi mượn sách chưa trả
            String checkBorrowingQuery = "SELECT id, borrow_date, due_date FROM borrowings WHERE username = ? AND book_id = ? AND return_date IS NULL";
            int borrowingId = -1;
            LocalDate borrowDate = null;
            LocalDate dueDate = null;

            try (PreparedStatement checkStmt = connection.prepareStatement(checkBorrowingQuery)) {
                checkStmt.setString(1, usernames);
                checkStmt.setInt(2, bookId);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    borrowingId = rs.getInt("id");
                    borrowDate = LocalDate.parse(rs.getString("borrow_date"));
                    dueDate = LocalDate.parse(rs.getString("due_date"));
                } else {
                    return "404";
                }
            }

            // Cập nhật ngày trả
            LocalDate returnDate = LocalDate.now();
            String updateReturnDateQuery = "UPDATE borrowings SET return_date = ? WHERE id = ?";
            try (PreparedStatement updateStmt = connection.prepareStatement(updateReturnDateQuery)) {
                updateStmt.setDate(1, Date.valueOf(returnDate));
                updateStmt.setInt(2, borrowingId);
                updateStmt.executeUpdate();
            }

            // Tăng số lượng sách trong bảng books
            String updateQuantityQuery = "UPDATE books SET quantity = quantity + 1 WHERE book_id = ?";
            try (PreparedStatement updateQuantityStmt = connection.prepareStatement(updateQuantityQuery)) {
                updateQuantityStmt.setInt(1, bookId);
                updateQuantityStmt.executeUpdate();
            }

            // Kiểm tra nếu ngày trả nằm trong khoảng borrow_date và due_date
            if ((returnDate.isEqual(borrowDate) || returnDate.isAfter(borrowDate)) &&
                    (returnDate.isEqual(dueDate) || returnDate.isBefore(dueDate))) {

                // Cập nhật điểm trong bảng tv_user
                String updatePointQuery = "UPDATE tv_user SET points = points + 10 WHERE username = ?";
                try (PreparedStatement updatePointStmt = connection.prepareStatement(updatePointQuery)) {
                    updatePointStmt.setString(1, usernames);
                    updatePointStmt.executeUpdate();
                }
            }

            return "200";
        } catch (SQLException e) {
            methodFunction.logException(e);
            return "404";
        }
    }
    /// tra ve lich su muon cua moi username
    public String getBorrowingHistoryByUsername(String targetUsername) {
        String query = "SELECT id, username, book_id, borrow_date, due_date, return_date FROM borrowings WHERE username = ?";
        ArrayList<BorrowingRecord> borrowingsList = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(jdbcURL, username, password);
             PreparedStatement statement = connection.prepareStatement(query)) {

            // Gán giá trị cho tham số `username` trong câu lệnh SQL
            statement.setString(1, targetUsername);
            try (ResultSet resultSet = statement.executeQuery()) {
                // Duyệt qua các bản ghi và lưu vào danh sách
                while (resultSet.next()) {
                    BorrowingRecord record = new BorrowingRecord();
                    record.setId(resultSet.getInt("id"));
                    record.setUsername(resultSet.getString("username"));
                    record.setBookId(resultSet.getInt("book_id"));
                    record.setBorrowDate(resultSet.getDate("borrow_date").toString());
                    record.setDueDate(resultSet.getDate("due_date").toString());

                    // Kiểm tra nếu sách đã được trả hay chưa
                    Date returnDate = resultSet.getDate("return_date");
                    record.setReturnDate(returnDate != null ? returnDate.toString() : null);

                    borrowingsList.add(record);
                }
            }
        } catch (SQLException e) {
            methodFunction.logException(e);

            return "404";
        }

        Gson gson = new Gson().newBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        return gson.toJson(borrowingsList);
    }
    // tim kiem theo keyword hien tai co book_id, title, author, category
    public String searchBooksByKeyword(String keyword) {
        String query = "SELECT book_id, title, author, description, quantity, image, category, exchange_point, viewers, borrowing_count FROM books " +
                "WHERE CAST(book_id AS CHAR) LIKE ? OR title LIKE ? OR author LIKE ? OR category LIKE ?";
        ArrayList<Books> booksList = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(jdbcURL, username, password);
             PreparedStatement statement = connection.prepareStatement(query)) {

            // Gán giá trị cho các tham số tìm kiếm với ký tự wildcard '%'
            String searchKeyword = "%" + keyword + "%";
            statement.setString(1, searchKeyword);
            statement.setString(2, searchKeyword);
            statement.setString(3, searchKeyword);
            statement.setString(4, searchKeyword); // Thêm tham số cho category

            try (ResultSet resultSet = statement.executeQuery()) {
                // Duyệt qua các bản ghi và lưu vào danh sách sách
                while (resultSet.next()) {
                    Books book = new Books();
                    book.setId(resultSet.getInt("book_id"));
                    book.setTitle(resultSet.getString("title"));
                    book.setAuthor(resultSet.getString("author"));
                    book.setDescription(resultSet.getString("description"));
                    book.setQuantity(resultSet.getInt("quantity"));
                    book.setImage(resultSet.getString("image"));
                    book.setCategory(resultSet.getString("category")); // Thêm category
                    book.setExchange_point(resultSet.getInt("exchange_point")); // Thêm exchange_point
                    book.setViewers(resultSet.getInt("viewers")); // Thêm viewers
                    booksList.add(book);
                }
            }
        } catch (SQLException e) {
            methodFunction.logException(e);
            return "404";
        }

        Gson gson = new Gson().newBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        return gson.toJson(booksList);
    }
    // tim kiem the loai nao cua user muon nhieu nhat
    public String getMostBorrowedCategory(String user) {
        String sql = """
            SELECT b.category, COUNT(*) AS borrow_count
            FROM borrowings br
            JOIN books b ON br.book_id = b.book_id
            WHERE br.username = ?
            GROUP BY b.category
            ORDER BY borrow_count DESC
            LIMIT 1;
            """;
        try (Connection connection = DriverManager.getConnection(jdbcURL, username, password);
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String category = rs.getString("category");
                    int borrowCount = rs.getInt("borrow_count");
                    System.out.println("Category: " + category + ", Borrow Count: " + borrowCount);
                    return category;
                } else {
                    System.out.println("No borrowings found for username: " + user);
                    return "404";
                }
            }
        } catch (Exception e) {
            methodFunction.logException(e);
            return "404";
        }
    }
    /// timf kiem dua vao view sap xep theo thu tu giam dan
    public String searchBooksByViewers() {
        String query = "SELECT book_id, title, author, description, quantity, image, category, exchange_point, viewers, borrowing_count FROM books " +
                "ORDER BY viewers DESC"; // Sắp xếp theo viewers giảm dần
        ArrayList<Books> booksList = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(jdbcURL, username, password);
             PreparedStatement statement = connection.prepareStatement(query)) {

            try (ResultSet resultSet = statement.executeQuery()) {
                // Duyệt qua các bản ghi và lưu vào danh sách sách
                while (resultSet.next()) {
                    Books book = new Books();
                    book.setId(resultSet.getInt("book_id"));
                    book.setTitle(resultSet.getString("title"));
                    book.setAuthor(resultSet.getString("author"));
                    book.setDescription(resultSet.getString("description"));
                    book.setQuantity(resultSet.getInt("quantity"));
                    book.setImage(resultSet.getString("image"));
                    book.setCategory(resultSet.getString("category")); // Thêm category
                    book.setExchange_point(resultSet.getInt("exchange_point")); // Thêm exchange_point
                    book.setViewers(resultSet.getInt("viewers")); // Thêm viewers
                    booksList.add(book);
                }
            }
        } catch (SQLException e) {
            methodFunction.logException(e);
            return "404"; // Trả về mảng rỗng nếu có lỗi
        }

        // Sử dụng Gson để chuyển đổi danh sách sách thành chuỗi JSON
        Gson gson = new Gson().newBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        return gson.toJson(booksList);
    }
    public String getUserBook(String username){
        String theloai=getMostBorrowedCategory(username);
        if(!theloai.equals("404")){
            return searchBooksByKeyword(theloai);
        }else return searchBooksByViewers();
    }

    //Show sách áp dụng để đổi
    public String getGiftBooks() {
        ArrayList<Books> books = new ArrayList<>();
        Gson gson = new Gson().newBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        try {
            Connection connection = DriverManager.getConnection(jdbcURL, username, password);
            String query = "SELECT * FROM books WHERE exchange_point > 0";
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("book_id");
                String title = resultSet.getString("title");
                String author = resultSet.getString("author");
                String description = resultSet.getString("description");
                int quantity = resultSet.getInt("quantity");
                String image = resultSet.getString("image");
                String category = resultSet.getString("category");
                int viewers = resultSet.getInt("viewers");
                int exchange_point = resultSet.getInt("exchange_point");
                int borrowing_count = resultSet.getInt("borrowing_count");
                books.add(new Books(id,title, author, description, quantity, image,category,viewers
                        ,exchange_point,borrowing_count));
            }
            resultSet.close();
            statement.close();
            connection.close();
        } catch (Exception e) {
            methodFunction.logException(e);
            return "404";
        }
        return gson.toJson(books);
    }

    //// note co the them trang sach da doi hien tai chua co
    public String exchangePoints(String user, int bookId) {
        try (Connection connection = DriverManager.getConnection(jdbcURL, username, password)) {
            // Lấy số điểm của người dùng và số điểm cần để đổi sách
            String checkPointsQuery = "SELECT points FROM tv_user WHERE username = ?";
            String checkExchangePointQuery = "SELECT exchange_point, quantity FROM books WHERE book_id = ?";

            int userPoints = 0;
            int bookPoints = 0;
            int bookQuantity = 0;

            // Lấy điểm của người dùng
            try (PreparedStatement userStmt = connection.prepareStatement(checkPointsQuery)) {
                userStmt.setString(1, user);
                ResultSet userRs = userStmt.executeQuery();
                if (userRs.next()) {
                    userPoints = userRs.getInt("points");
                } else {
                    return "404";
                }
            }

            // Lấy số điểm để đổi sách và số lượng sách
            try (PreparedStatement bookStmt = connection.prepareStatement(checkExchangePointQuery)) {
                bookStmt.setInt(1, bookId);
                ResultSet bookRs = bookStmt.executeQuery();
                if (bookRs.next()) {
                    bookPoints = bookRs.getInt("exchange_point");
                    bookQuantity = bookRs.getInt("quantity");
                } else {
                    return "404"; // Sách không tồn tại
                }
            }
            // Kiểm tra điểm của người dùng và số lượng sách còn lại
            if (userPoints < bookPoints) {
                return "103"; // Không đủ điểm
            }
            if (bookQuantity <= 0) {
                return "110"; // Hết sách để đổi
            }

            // Trừ điểm người dùng
            String updateUserPointsQuery = "UPDATE tv_user SET points = points - ? WHERE username = ?";
            try (PreparedStatement updateUserStmt = connection.prepareStatement(updateUserPointsQuery)) {
                updateUserStmt.setInt(1, bookPoints);
                updateUserStmt.setString(2, user);
                updateUserStmt.executeUpdate();
            }

            // Giảm số lượng sách
            String updateBookQuantityQuery = "UPDATE books SET quantity = quantity - 1 WHERE book_id = ?";
            try (PreparedStatement updateBookStmt = connection.prepareStatement(updateBookQuantityQuery)) {
                updateBookStmt.setInt(1, bookId);
                updateBookStmt.executeUpdate();
            }

            return "200";
        } catch (SQLException e) {
            methodFunction.logException(e);
            return "404";
        }
    }

    // cap nhap thong tin ben user, doi mk
    public boolean updateUser(String jsonInput) {
        try {
            // Phân tích JSON
            //  Gson gson = new Gson();
            JsonObject jsonObject = JsonParser.parseString(jsonInput).getAsJsonObject();

            // Lấy thông tin từ JSON
            String user = jsonObject.get("username").getAsString();
            String name = jsonObject.has("name") ? jsonObject.get("name").getAsString() : null;
            String phone = jsonObject.has("phone") ? jsonObject.get("phone").getAsString() : null;
            String pass = jsonObject.has("password") ? jsonObject.get("password").getAsString() : null;

            // Kiểm tra nếu không có username
            if (user == null || user.isEmpty()) {
                return false;
            }


            // Chuẩn bị câu lệnh SQL
            StringBuilder sql = new StringBuilder("UPDATE tv_user SET ");
            boolean hasUpdate = false;

            if (name != null) {
                sql.append("name = ?, ");
                hasUpdate = true;
            }
            if (phone != null) {
                sql.append("phone = ?, ");
                hasUpdate = true;
            }
            if (pass != null) {
                sql.append("password = ?, ");
                hasUpdate = true;
            }

            if (!hasUpdate) {
                return false;
            }

            // Xóa dấu phẩy cuối cùng và thêm điều kiện WHERE
            sql.setLength(sql.length() - 2);
            sql.append(" WHERE username = ?");

            // Kết nối cơ sở dữ liệu
            try (Connection conn =  DriverManager.getConnection(jdbcURL, username, password);
                 PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

                // Gán giá trị vào câu lệnh SQL
                int index = 1;
                if (name != null) {
                    stmt.setString(index++, name);
                }
                if (phone != null) {
                    stmt.setString(index++, phone);
                }
                if (pass != null) {
                    stmt.setString(index++, pass);
                }
                stmt.setString(index, user);

                // Thực thi câu lệnh
                int rowsUpdated = stmt.executeUpdate();
                if(rowsUpdated>0)
                {
                    sendNotificationToUser("You have changed information successfully.",user);
                }
                return rowsUpdated > 0;
            }
        } catch (Exception e) {
            methodFunction.logException(e);
        }
        return false;
    }



// <----------------Thông báo ------------------->

    public String sendNotificationToUser(String message, String user) {
        try (Connection connection = DriverManager.getConnection(jdbcURL, username, password)) {
            String query = "INSERT INTO notifications (message, username, is_read) VALUES (?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, message); // Nội dung thông báo
                statement.setString(2, user); // Người nhận là username cụ thể
                statement.setBoolean(3, false); // Đánh dấu là chưa đọc
                int rowsAffected = statement.executeUpdate();
                if (rowsAffected > 0) {
                    return "200"; // Thông báo gửi thành công
                } else {
                    return "404"; // Nếu không gửi được thông báo
                }
            }
        } catch (SQLException e) {
            methodFunction.logException(e);
            return "404"; // Lỗi SQL
        }
    }
    public String sendNotificationToAll(String message) {
        try (Connection connection = DriverManager.getConnection(jdbcURL, username, password)) {
            String query = "INSERT INTO notifications (message, username, is_read) VALUES (?, NULL, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, message); // Nội dung thông báo
                statement.setBoolean(2, false); // Đánh dấu là chưa đọc
                int rowsAffected = statement.executeUpdate();
                if (rowsAffected > 0) {
                    return "200"; // Thông báo gửi thành công
                } else {
                    return "404"; // Nếu không gửi được thông báo
                }
            }
        } catch (SQLException e) {
            methodFunction.logException(e);
            return "404"; // Lỗi SQL
        }
    }
    public String getNotificationsForUser(String user) {
        try (Connection connection = DriverManager.getConnection(jdbcURL, username, password)) {
            String query = "SELECT * FROM notifications WHERE username = ? OR username IS NULL ORDER BY created_at DESC";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, user);
                ResultSet rs = statement.executeQuery();

                ArrayList<Notification> notifications = new ArrayList<>();
                while (rs.next()) {
                    String message = rs.getString("message");
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    boolean isRead = rs.getBoolean("is_read");

                    Notification notification = new Notification(rs.getInt("id"), message, rs.getString("username"), isRead, createdAt);
                    notifications.add(notification);
                }

                if (!notifications.isEmpty()) {
                    Gson gson = new Gson().newBuilder().disableHtmlEscaping().setPrettyPrinting().create();
                    return gson.toJson(notifications);
                } else {
                    return "[]";
                }
            }
        } catch (SQLException e) {
            methodFunction.logException(e);
            return "404";
        }
    }
    public String markNotificationAsRead(int notificationId) {
        try (Connection connection = DriverManager.getConnection(jdbcURL, username, password)) {
            String query = "UPDATE notifications SET is_read = TRUE WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, notificationId);
                int rowsAffected = statement.executeUpdate();
                if (rowsAffected > 0) {
                    return "200";
                } else {
                    return "404";
                }
            }
        } catch (SQLException e) {
            methodFunction.logException(e);
            return "404"; // Lỗi SQL
        }
    }

    public String markAllNotificationsAsRead(String user) {
        try (Connection connection = DriverManager.getConnection(jdbcURL, username, password)) {
            String query = "UPDATE notifications SET is_read = TRUE WHERE username = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, user);
                int rowsAffected = statement.executeUpdate();
                if (rowsAffected > 0) {
                    return "200"; // Đã cập nhật thành công
                } else {
                    return "404"; // Không có thông báo nào để cập nhật
                }
            }
        } catch (SQLException e) {
            methodFunction.logException(e);
            return "404"; // Lỗi SQL
        }
    }


    // Đổi mk
    public String changePassword(String user, String oldPass, String newPass) {
        String sqlCheckUser = "SELECT password FROM account WHERE username = ?";
        String sqlUpdatePassword = "UPDATE account SET password = ? WHERE username = ?";

        try (Connection connection = DriverManager.getConnection(jdbcURL, username, password);
             PreparedStatement checkStmt = connection.prepareStatement(sqlCheckUser)) {

            checkStmt.setString(1, user);
            ResultSet resultSet = checkStmt.executeQuery();

            if (!resultSet.next()) {
                return "503"; // User không tồn tại
            }

            String hashedOldPassword = resultSet.getString("password");
            if (!hashedOldPassword.equals(methodFunction.MD5_transmit(oldPass))) {
                return "505"; // Mật khẩu cũ không đúng
            }

            // Mã hóa mật khẩu mới
            String hashedNewPassword = methodFunction.MD5_transmit(newPass);

            try (PreparedStatement updateStmt = connection.prepareStatement(sqlUpdatePassword)) {
                updateStmt.setString(1, hashedNewPassword);
                updateStmt.setString(2, user);
                updateStmt.executeUpdate();
            }

            return "200"; // Đổi mật khẩu thành công

        } catch (SQLException e) {
            methodFunction.logException(e);
            return "500"; // Lỗi hệ thống
        }
    }

    //Quen mk
    public String resetPassword(String email) {
        String sqlCheckEmail = "SELECT username FROM account WHERE email = ?";
        String sqlUpdatePassword = "UPDATE account SET password = ? WHERE email = ?";

        try (Connection connection = DriverManager.getConnection(jdbcURL, username, password);
             PreparedStatement checkStmt = connection.prepareStatement(sqlCheckEmail)) {

            checkStmt.setString(1, email);
            ResultSet resultSet = checkStmt.executeQuery();

            if (!resultSet.next()) {
                return "504";
            }

            // Đặt lại mật khẩu thành "123" (đã mã hóa MD5)
            String newPassword = methodFunction.MD5_transmit("123");

            try (PreparedStatement updateStmt = connection.prepareStatement(sqlUpdatePassword)) {
                updateStmt.setString(1, newPassword);
                updateStmt.setString(2, email);
                updateStmt.executeUpdate();
            }

            return "200"; // Thành công

        } catch (SQLException e) {
            methodFunction.logException(e);
            return "500"; // Lỗi hệ thống
        }
    }




}
