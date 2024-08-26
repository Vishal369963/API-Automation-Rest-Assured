package com.example.tests;

import io.restassured.RestAssured;
import java.util.HashMap;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BookingTest5 {
    private static final String BASE_URL = "https://restful-booker.herokuapp.com";
    private static List<Map<String, Object>> bookings;
    private static List<String> bookingIds = new ArrayList<>();

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
    public void createMultipleBookings() {
        for (Map<String, Object> booking : bookings) {
            Response response = RestAssured.given()
                    .contentType("application/json")
                    .body(booking)
                    .post("/booking");

            response.print();
            Assert.assertEquals(response.statusCode(), 200);

            String bookingId = response.jsonPath().getString("bookingid");
            Assert.assertNotNull(bookingId);
            bookingIds.add(bookingId);
        }
    }

    @Test(dependsOnMethods = "createMultipleBookings")
    public void getMultipleBookings() {
        for (int i = 0; i < bookingIds.size(); i++) {
            String bookingId = bookingIds.get(i);
            Map<String, Object> expectedBooking = bookings.get(i);

            // Retrieve the booking details
            Response response = RestAssured.given()
                    .get("/booking/" + bookingId);
            response.print();

            Assert.assertEquals(response.statusCode(), 200);

            // Validate response content
            String actualFirstName = response.jsonPath().getString("firstname");
            String expectedFirstName = (String) expectedBooking.get("firstname");

            Assert.assertEquals(actualFirstName, expectedFirstName, "Mismatch in firstname for booking ID: " + bookingId);
        }
    }


    @Test(dependsOnMethods = "getMultipleBookings")
    public void updateMultipleBookings() {
        for (int i = 0; i < bookingIds.size(); i++) {
            String bookingId = bookingIds.get(i);
            Map<String, Object> booking = bookings.get(i);

            // Prepare update data (e.g., change the last name)
            Map<String, Object> updatedBooking = new HashMap<>(booking);
            updatedBooking.put("lastname", "Singh" + i); // Update last name with a unique value

            Response response = RestAssured.given()
                    .auth().preemptive().basic("admin", "password123") // Replace with actual username and password
                    .contentType("application/json")
                    .body(updatedBooking)
                    .put("/booking/" + bookingId);
            response.print();

            Assert.assertEquals(response.statusCode(), 200);

            // Verify update
            Response getResponse = RestAssured.given()
                    .get("/booking/" + bookingId);


            Assert.assertEquals(getResponse.statusCode(), 200);
            Assert.assertEquals(getResponse.jsonPath().getString("lastname"), "Singh" + i);
        }
    }


    @Test(dependsOnMethods = "updateMultipleBookings")
    public void deleteMultipleBookings() {
        for (String bookingId : bookingIds) {
            // Send DELETE request to delete the booking
            Response response = RestAssured.given()
                    .auth()
                    .preemptive()
                    .basic("admin", "password123") // Replace with actual username and password
                    .delete("/booking/" + bookingId);
            response.print();

            // Verify the correct status code for deletion
            Assert.assertTrue(response.statusCode() == 201,
                    "Expected status code 201, but got " + response.statusCode());

            // Verify the booking has been deleted by checking the status code for the GET request
            Response getResponse = RestAssured.given()
                    .get("/booking/" + bookingId);
            getResponse.print();

            Assert.assertEquals(getResponse.statusCode(), 404,
                    "Expected status code 404, but got " + getResponse.statusCode());
        }
    }

}

