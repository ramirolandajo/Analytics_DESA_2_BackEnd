package ar.edu.uade.analytics.Entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void isEmptyWhenNew() {
        User u = new User();
        assertTrue(u.isEmpty(), "New user should be empty");
    }

    @Test
    void isEmptyWhenFieldsSet() {
        User u = new User();
        u.setId(1);
        u.setName("Nombre");
        u.setLastname("Apellido");
        u.setEmail("a@b.com");
        u.setPassword("secret");
        u.setRole("USER");
        assertFalse(u.isEmpty(), "User with fields set should not be empty");
    }

    @Test
    void sessionActiveDefaultAndToggle() {
        User u = new User();
        assertFalse(u.getSessionActive(), "Default sessionActive should be false");
        u.setSessionActive(true);
        assertTrue(u.getSessionActive(), "sessionActive should be true after set");
    }

    @Test
    void partialFieldsMakeNotEmpty() {
        User u = new User();
        u.setName("SoloNombre");
        assertFalse(u.isEmpty(), "User with name only should not be empty because isEmpty checks multiple fields");
    }

    @Test
    void accountActiveDefaultAndToggle() {
        User u = new User();
        assertFalse(u.isAccountActive(), "Default accountActive should be false");
        u.setAccountActive(true);
        assertTrue(u.isAccountActive(), "accountActive should be true after set");
    }

    @Test
    void passwordAndEmailSetters() {
        User u = new User();
        u.setPassword("pwd123");
        u.setEmail("test@example.com");
        assertEquals("pwd123", u.getPassword());
        assertEquals("test@example.com", u.getEmail());
    }

    @Test
    void listsSettersAndGetters() {
        User u = new User();
        Cart c = new Cart();
        c.setId(5);
        Purchase p = new Purchase();
        p.setId(7);

        u.setCarts(List.of(c));
        u.setPurchases(List.of(p));
        u.setFavouriteProducts(List.of(new FavouriteProducts()));

        assertEquals(1, u.getCarts().size());
        assertEquals(5, u.getCarts().get(0).getId());
        assertEquals(1, u.getPurchases().size());
        assertEquals(7, u.getPurchases().get(0).getId());
        assertEquals(1, u.getFavouriteProducts().size());
    }

    @Test
    void jsonSerializationHidesPassword() throws Exception {
        User u = new User();
        u.setId(11);
        u.setName("N");
        u.setEmail("e@x.com");
        u.setPassword("topsecret");

        ObjectMapper m = new ObjectMapper();
        String json = m.writeValueAsString(u);

        assertFalse(json.contains("topsecret"), "Serialized JSON should not contain password value");
        assertFalse(json.contains("\"password\""), "Serialized JSON should not contain the password key");
        assertTrue(json.contains("e@x.com"));
    }

    @Test
    void jsonDeserializationDoesNotSetPassword() throws Exception {
        String json = "{\"id\":22, \"name\":\"N2\", \"email\":\"a@b.com\", \"password\":\"leak\"}";
        ObjectMapper m = new ObjectMapper();
        User u = m.readValue(json, User.class);
        // password is annotated with @JsonIgnore, so it should not be set during deserialization
        assertNull(u.getPassword(), "Password should not be set by deserialization when field is @JsonIgnore");
        assertEquals(22, u.getId());
        assertEquals("a@b.com", u.getEmail());
    }

    @Test
    void idOnlyIsNotEmpty() {
        User u = new User();
        u.setId(99);
        assertFalse(u.isEmpty(), "User with only id set should not be empty");
    }

    @Test
    void emailNullButPasswordSetIsNotEmpty() {
        User u = new User();
        u.setPassword("pw");
        u.setEmail(null);
        assertFalse(u.isEmpty(), "User with password set should not be empty even if email null");
    }

    @Test
    void listsNullHandlingAndRoleLastname() {
        User u = new User();
        u.setCarts(null);
        u.setPurchases(null);
        u.setFavouriteProducts(null);
        u.setLastname("LName");
        u.setRole("ADMIN");

        assertNull(u.getCarts());
        assertNull(u.getPurchases());
        assertNull(u.getFavouriteProducts());
        assertEquals("LName", u.getLastname());
        assertEquals("ADMIN", u.getRole());
    }

    // --- new branch tests ---
    @Test
    void isEmptyWithEmptyStrings() {
        User u = new User();
        u.setName("");
        u.setLastname("");
        u.setEmail("");
        u.setPassword("");
        u.setRole("");
        // isEmpty checks for null, empty strings are considered set => not empty
        assertFalse(u.isEmpty(), "User with empty strings should not be considered empty");
    }

    @Test
    void onlyPasswordSetIsNotEmpty() {
        User u = new User();
        u.setPassword("onlypwd");
        assertFalse(u.isEmpty(), "User with only password set should not be empty");
    }

    @Test
    void sessionActiveCanBeNull() {
        User u = new User();
        u.setSessionActive(null);
        assertNull(u.getSessionActive(), "sessionActive should accept null and return null");
    }

    @Test
    void lastnameOnlyMakesNotEmpty() {
        User u = new User();
        u.setLastname("SoloApellido");
        assertFalse(u.isEmpty(), "User with only lastname should not be empty");
    }

    @Test
    void emailOnlyMakesNotEmpty() {
        User u = new User();
        u.setEmail("solo@e.com");
        assertFalse(u.isEmpty(), "User with only email should not be empty");
    }

    @Test
    void roleOnlyMakesNotEmpty() {
        User u = new User();
        u.setRole("ROLE");
        assertFalse(u.isEmpty(), "User with only role should not be empty");
    }
}
