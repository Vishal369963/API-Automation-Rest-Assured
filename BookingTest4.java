package com.example.tests;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BookingTest4 {
    private static final String BASE_URL = "https://restful-booker.herokuapp.com";
    private static List<Map<String, Object>> bookings;
    private static String bookingId;

    @BeforeClass
    public void setUp() {
        RestAssured.baseURI = BASE_URL;
        try {
            // Read JSON array from file
            String json = FileUtils.readFileAsString("src/test/resources/requestBody.json");
            ObjectMapper mapper = new ObjectMapper();
            bookings = mapper.readValue(json, List.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void createBooking() {
        // Create booking from the first object in the list
        Map<String, Object> booking = bookings.get(1);
        Response response = RestAssured.given()
                .contentType("application/json")
                .body(booking)
                .post("/booking");
        response.print();

        Assert.assertEquals(response.statusCode(), 200);
        bookingId = response.jsonPath().getString("bookingid");
        Assert.assertNotNull(bookingId);
    }

    @Test(dependsOnMethods = "createBooking")
    public void getBooking() {
        // Get the booking details
        Response response = RestAssured.given()
                .get("/booking/" + bookingId);
        response.print();

        Assert.assertEquals(response.statusCode(), 200);
        // Optionally validate response content
        Assert.assertEquals(response.jsonPath().getString("firstname"), bookings.get(1).get("firstname"));
    }

    @Test(dependsOnMethods = "getBooking")
    public void updateBooking() {
        // Prepare update data (e.g., change the last name)
        Map<String, Object> updatedBooking = bookings.get(1);
        updatedBooking.put("lastname", "Singh");

        Response response = RestAssured.given()
                .auth().preemptive().basic("admin", "password123")
                .contentType("application/json")
                .body(updatedBooking)
                .put("/booking/" + bookingId);


        Assert.assertEquals(response.statusCode(), 200);

        // Verify update
        Response getResponse = RestAssured.given()
                .get("/booking/" + bookingId);
        getResponse.print();

        Assert.assertEquals(getResponse.statusCode(), 200);
        Assert.assertEquals(getResponse.jsonPath().getString("lastname"), "Singh");
    }

    @Test(dependsOnMethods = "updateBooking")
    public void deleteBooking() {
        // Delete the booking
        Response response = RestAssured.given()
                .auth()
                .preemptive()
                .basic("admin", "password123") // Replace with actual username and password
                .delete("/booking/" + bookingId);
        response.print();

        // Verify the correct status code for deletion based on API documentation
        Assert.assertEquals(response.statusCode(), 201); // or 204

        // Verify deletion
        Response getResponse = RestAssured.given()
                .get("/booking/" + bookingId);

        Assert.assertEquals(getResponse.statusCode(), 404);
    }
}
