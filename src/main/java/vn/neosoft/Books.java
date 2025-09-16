package vn.neosoft;

public class Books {
    private int id;
    private String title;
    private String author;
    private String description;
    private int quantity;
    private String image;
    private String category;
    private int viewers;
    private int exchange_point;
    private int borrowing_count;

    // Constructor


    public Books() {
    }

    public Books(int id, String title, String author, String description, int quantity, String image, String category, int viewers, int exchange_point, int borrowing_count) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.description = description;
        this.quantity = quantity;
        this.image = image;
        this.category = category;
        this.viewers = viewers;
        this.exchange_point = exchange_point;
        this.borrowing_count = borrowing_count;
    }


    // Getters và Setters

    public int getBorrowing_count() {
        return borrowing_count;
    }

    public void setBorrowing_count(int borrowing_count) {
        this.borrowing_count = borrowing_count;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getViewers() {
        return viewers;
    }

    public void setViewers(int viewers) {
        this.viewers = viewers;
    }

    public int getExchange_point() {
        return exchange_point;
    }

    public void setExchange_point(int exchange_point) {
        this.exchange_point = exchange_point;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
