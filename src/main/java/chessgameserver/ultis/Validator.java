package chessgameserver.ultis;


public class Validator {
    public static String userNameValidator(String userName) {
        if (userName == null || userName.isEmpty()) {
            return "Username cannot be empty";
        }
        if(userName.length() < 6 || userName.length() > 20) {
            return "Username must be between 6 and 20 characters";
        }

        if(!userName.matches("^[a-zA-Z0-9]*$")) {
            return "Username must contain only letters and numbers";
        }
    
        return "ok";
    }

    public static String passwordValidator(String password) {
        if (password == null || password.isEmpty()) {
            return "Password cannot be empty";
        }
        if(password.length() < 6 || password.length() > 20) {
            return "Password must be between 6 and 20 characters";
        }

        if(!password.matches("^[a-zA-Z0-9]*$")) {
            return "Password must contain only letters and numbers";
        }
    
        return "ok";
    }
    
    public static String emailValidator(String email) {
        if (email == null || email.isEmpty()) {
            return "Email cannot be empty";
        }
        if(!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            return "Invalid email format";
        }
    
        return "ok";
    }
}
