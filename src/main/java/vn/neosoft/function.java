package vn.neosoft;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

interface functon_manager{
    void logException(Exception e); // ghi file exception chỉ lấy dòng đầu
    void logException1(String ma,Exception e);// ghi thêm mã và exception
    String getFirstStackTraceLine(Exception e);// chỉ lấy dòng đâu exception
    String MD5_transmit(String input); //ma hóa md5
    boolean isValidPassword(String password) ;

}

public class function implements functon_manager{
       private static String duongdanlog="D:\\NEOSOFT\\LEARNING\\Code\\Library\\log_system.log";
       //static String duongdanlog="/var/lib/tomcat10/webapps/log_system.txt";
    private static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{4,}$";
    public void logException(Exception e) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        try (PrintWriter writer = new PrintWriter(new FileWriter(duongdanlog, true))) {
            writer.print("Time: " + timeStamp);
            writer.print(" | Exception:");
            writer.println(getFirstStackTraceLine(e));
        } catch (Exception ex) {
            //ex.printStackTrace();
        }}
    public void log(String messagee) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        try (PrintWriter writer = new PrintWriter(new FileWriter(duongdanlog, true))) {
            writer.print("Time: " + timeStamp+"-");
            writer.println(messagee);
        } catch (Exception ex) {
            //ex.printStackTrace();
        }}
    public void logException1(String ma,Exception e) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        try (PrintWriter writer = new PrintWriter(new FileWriter(duongdanlog, true))) {
            writer.print("Time: " + timeStamp);
            writer.print(" " +ma+" | Exception:");
            writer.println(getFirstStackTraceLine(e));
        } catch (Exception ex) {
        }}
    //
    public String getFirstStackTraceLine(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String[] lines = sw.toString().split("\n");
        return lines.length > 0 ? lines[0].trim() : "";
    }


    public String MD5_transmit(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            byte[] messageDigest = md.digest(input.getBytes());

            BigInteger no = new BigInteger(1, messageDigest);

            StringBuilder hashText = new StringBuilder(no.toString(16));

            while (hashText.length() < 32) {
                hashText.insert(0, "0");
            }

            return hashText.toString();
        } catch (NoSuchAlgorithmException e) {
            logException(e);
        }

        return "";
    }

    public boolean isValidPassword(String password) {
        return Pattern.matches(PASSWORD_PATTERN, password);
    }



}
