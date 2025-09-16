package vn.neosoft;


import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.*;

@RestController
public class LibController {
    SQL sql=new SQL();
    // Xác thực đăng ký
    @PostMapping("/auth/register")
    public String registerUser(@RequestParam(required = false) String username,
                                               @RequestParam(required = false) String password,
                                               @RequestParam(required = false) String name,
                                               @RequestParam(required = false) String role,
                                               @RequestParam (required = false) String phone) {
        int isRegistered = sql.registerUser(username, password, name, role, phone);
        System.out.println(isRegistered);
        if(username.isEmpty() || password.isEmpty() || name.isEmpty() || role.isEmpty()
        || phone.isEmpty())
        {

            return "404";
        }
        if (isRegistered == 200) {
            return "200";
        } else {
            return "400";
        }
    }

    // Xác thực đăng nhập
    @PostMapping("/auth/login")
    public String loginUser(@RequestParam String username,
                            @RequestParam String password,
                            HttpSession session) {
        String data = sql.loginUser(username, password,session);
        if (data == null && data.isEmpty()) {

            return "404";
        }
        else if(data.equals("404"))
        {
            return "400";
        }
        else if (data.equals("403"))
        {
            return "403";// bị khóa
        }
        else
            return data;
    }
    // <----------------Notifications----------------->
    // GỬi thông bao cho username
    @GetMapping("/send-notification/")
    public String sendNotificationToUser(@RequestParam String message, @RequestParam String username) {
        String result = sql.sendNotificationToUser(message, username);
        if ("200".equals(result)) {
            return "Notification sent to " + username;
        } else {
            return "Error sending notification";
        }
    }
// Gửi tấtca3
    @GetMapping("/send-notification/all")
    public String sendNotificationToAll(@RequestParam String message) {
        String result = sql.sendNotificationToAll(message);
        if ("200".equals(result)) {
            return "Notification sent to all users";
        } else {
            return "Error sending notification";
        }
    }

    @GetMapping("/notifications/")
    public String getNotificationsForUser(@RequestParam String username) {
        return sql.getNotificationsForUser(username);
    }
// Đánh dấu đã đọc
    @GetMapping("/notifications/mark-read/")
    public String markNotificationAsRead(@RequestParam int notificationId) {
        String result = sql.markNotificationAsRead(notificationId);
        if ("200".equals(result)) {
            return "Notification with ID " + notificationId + " marked as read";
        } else {
            return "Error marking notification as read";
        }
    }
    @GetMapping("/notifications/mark-all-read/")
    public String markAllNotificationsAsRead(@RequestParam String username) {
        String result = sql.markAllNotificationsAsRead(username);
        if ("200".equals(result)) {
            return "All notifications for user " + username + " marked as read";
        } else {
            return "Error marking notifications as read";
        }
    }





    // <--------------Guest--------------------->
    //Tăng view khi ấn description
    @PostMapping("/admin/increase-view")
    public String increase_view(@RequestParam String title,
                                @RequestParam boolean status)
    {
        int data = sql.updateView(title,status);
        if(data == 200)
        {
            return "200";
        }
        else
        {
            return "404";
        }
    }
    // Thể loại sách
    @GetMapping("/admin/category")
    public String getCategory()
    {
        return sql.getAllCategories();
    }

    // Khóa/Mở/Check status tài khoản
    @GetMapping("/admin/account-action")
    public String accountAction(
            @RequestParam String username,
            @RequestParam String action) {
        String status = sql.checkAccountStatus(username);
        if (status == null) {
            return "404";
        }
        if ("lock".equalsIgnoreCase(action)) {
            if ("true".equalsIgnoreCase(status)) {
                return "403";
            } else {
                int result = sql.lockUser(username);
                return result == 200 ? "200" : "400";
            }
        } else if ("unlock".equalsIgnoreCase(action)) {
            if ("false".equalsIgnoreCase(status)) {
                return "403";
            } else {
                int result = sql.unlockUser(username);
                return result == 200 ? "200" : "400";
            }
        }
        return "400";
    }

    // Danh sách lịch sử mượn sách của người dùng
    @GetMapping("/admin/history")
    public String getHistory()
    {
        String res = sql.getBorrowingHistory();
        if(res.equals("404") || res.equals("[]"))
        {
            return "404";
        }
        else
            return res;
    }
    // Danh sách người dùng
    @GetMapping("/admin/user-list")
    public String getUserList()
    {
        String res = sql.getUser();
        if(res.equals("404") || res.equals("[]"))
        {
            return "404";
        }
        else
            return res;
    }

    // Thêm 1 sách
    @PostMapping("/admin/add")
    @ResponseBody
    public String addBook(
            @RequestParam String title,
            @RequestParam String author,
            @RequestParam String description,
            @RequestParam int quantity,
            @RequestParam String image,
            @RequestParam String category) {
        int result = sql.addBook(title, author, description, quantity, image,category);
        if (result == 200) {

            return "200";
        } else {
            return "400";
        }
    }

    // Thêm nhiều sách
    @PostMapping("/admin/adds")
    @ResponseBody
    public String addBooks(@RequestParam String keyword, @RequestParam String quantity_res) {
        int result = sql.addBooks(keyword, quantity_res);
        if (result == 200) {
            return "200";
        } else {
            return "400";
        }
    }

    // Xóa sách
    @DeleteMapping("/admin/delete")
    @ResponseBody
    public String deleteBook(@RequestParam String title) {
        int result = sql.deleteBook(title);
        if (result == 200) {
            return "200";
        } else {
            return "400";
        }
    }

    // Chỉnh sửa sách
    @PutMapping("/admin/edit")
    @ResponseBody
    public String editBook(
            @RequestParam String title,
            @RequestParam String newTitle,
            @RequestParam String newAuthor,
            @RequestParam String newDes,
            @RequestParam int newQuantity,
            @RequestParam String newImage) {
        int result = sql.editBook(title, newTitle, newAuthor, newDes, newQuantity, newImage);
        if (result == 200) {
            return "200";
        } else {
            return "400";
        }
    }

    // Tìm kiếm sách dựa vào từ khóa(id sách, tác giả, tựa đề)
    @GetMapping("/admin/search")
    public String searchBookByTitle(@RequestParam String title) {
        String result = sql.searchBooksByKeyword(title);
        if (!"404".equals(result) || !"[]".equals(result)) {
            return result;
        } else {
            return "400";
        }
    }
    // Danh sach sách
    @GetMapping("/admin/show")
    public String getAllBooks() {
        String result = sql.searchBooksByViewers();
        if (!"404".equals(result)) {
            return result;
        } else {
            return "400";
        }
    }


    //<-------------USER------------->
    // Lấy thông tin
    @GetMapping("/user/info")
    public String getInfo(@RequestParam String username)
    {
        return sql.getInfo(username);
    }
    // Mượn sách
    @PostMapping("/user/borrow")
    public String borrow(@RequestParam(required = false) String username,
                         @RequestParam(required = false) Integer book_id) {
        int res = Integer.parseInt(sql.borrowBook(username, book_id));
        if (res == 100) {
            return "100";
        } else if (res == 404 || username.isEmpty()) {
            return "404";
        } else if(res == 101)
        {
            return "101";
        }
        else {
            sql.sendNotificationToUser("You have just successfully borrowed the book - "+book_id+".",username);
            return "200";
        }
    }


    // Trà sách
    @PostMapping("/user/return")
    public String returnBook(@RequestParam(required = false) String username,
                             @RequestParam(required = false) int book_id)
    {
        int res = Integer.parseInt(sql.returnBook(username,book_id));
        if(res == 404 )
        {
            return "404";
        }
        else {
            sql.sendNotificationToUser("You have just successfully returned the book - " + book_id + ".", username);
            return "200";
        }
    }

    // Lịch sử mượn sách
    @GetMapping("/user/history")
    public String getHistory(@RequestParam(required = false) String username)
    {
        String res = sql.getBorrowingHistoryByUsername(username);
        if(res.equals("404") || res.equals("[]"))
        {
            return "404";
        }
        else
            return res;
    }
    //Đổi điểm

    //Đổi thông tin
    @PutMapping("/user/update")
    public String updateUser(@RequestBody String jsonInput) {

        boolean isUpdated = sql.updateUser(jsonInput);

        if (isUpdated) {

            return "200";
        } else {
            return "404";
        }
    }
    // Gợi ý
    @GetMapping("/user/recommend")
    public String getUserBook(@RequestParam String username) {
        String result =sql.getUserBook(username);
        return result;
    }

    // TRANG GIFT
    @GetMapping("/gift/show")
    public String show()
    {
        return sql.getGiftBooks();
    }

    @PostMapping("/user/exchange-points")
    public String exchangePoints(@RequestParam String username, @RequestParam int bookId) {

        String res =sql.exchangePoints(username, bookId);
        System.out.println(res);
        if(res.equals("404"))
        {
            return "404";
        }
        else if(res.equals("110"))
        {
            return "110";
        }
        else if(res.equals("103"))
        {
            return "103";
        }
        else
        {
            sql.sendNotificationToUser("You have exchanged point successfully - "+bookId+".",username);
            return res;
        }
    }


}
