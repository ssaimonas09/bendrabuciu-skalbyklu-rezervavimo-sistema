public record Account(User user, String role) {
    public boolean isAdmin() {
        return role.equals("ADMIN");
    }
}
