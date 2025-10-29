package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.UCSBDiningCommonsMenuItem;
import edu.ucsb.cs156.example.repositories.UCSBDiningCommonsMenuItemRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = UCSBDiningCommonsMenuItemController.class)
@Import(TestConfig.class)
public class UCSBDiningCommonsMenuItemControllerTests extends ControllerTestCase {

  @MockBean UCSBDiningCommonsMenuItemRepository ucsbDiningCommonsMenuItemRepository;

  @MockBean UserRepository userRepository;

  // Authorization tests for /api/UCSBDiningCommonsMenuItem/admin/all

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc
        .perform(get("/api/UCSBDiningCommonsMenuItem/all"))
        .andExpect(status().is(403)); // logged out users can't get all
  }

  @Test
  public void logged_out_users_cannot_get_by_id() throws Exception {
    mockMvc
        .perform(get("/api/UCSBDiningCommonsMenuItem?id=7"))
        .andExpect(status().is(403)); // logged out users can't get by id
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc
        .perform(get("/api/UCSBDiningCommonsMenuItem/all"))
        .andExpect(status().is(200)); // logged
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {
    // arrange
    UCSBDiningCommonsMenuItem menu_item =
        UCSBDiningCommonsMenuItem.builder()
            .dining_commons_code("ortega")
            .name("Mac-n-Cheese")
            .station("East Plate")
            .build();
    when(ucsbDiningCommonsMenuItemRepository.findById(eq(7L))).thenReturn(Optional.of(menu_item));
    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/UCSBDiningCommonsMenuItem?id=7"))
            .andExpect(status().isOk())
            .andReturn();
    // assert
    verify(ucsbDiningCommonsMenuItemRepository, times(1)).findById(eq(7L));
    String expectedJson = mapper.writeValueAsString(menu_item);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {
    // arrange
    when(ucsbDiningCommonsMenuItemRepository.findById(eq(7L))).thenReturn(Optional.empty());
    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/UCSBDiningCommonsMenuItem?id=7"))
            .andExpect(status().isNotFound())
            .andReturn();
    // assert
    verify(ucsbDiningCommonsMenuItemRepository, times(1)).findById(eq(7L));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("UCSBDiningCommonsMenuItem with id 7 not found", json.get("message"));
  }

  // Authorization tests for /api/UCSBDiningCommonsMenuItem/post
  // (Perhaps should also have these for put and delete)

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc.perform(post("/api/UCSBDiningCommonsMenuItem/post")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(post("/api/UCSBDiningCommonsMenuItem/post"))
        .andExpect(status().is(403)); // only admins can post
  }

  // // Tests with mocks for database actions

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_menu_items() throws Exception {
    // arrange
    UCSBDiningCommonsMenuItem menu_item_1 =
        UCSBDiningCommonsMenuItem.builder()
            .dining_commons_code("ortega")
            .name("Mac-n-Cheese")
            .station("East Plate")
            .build();

    UCSBDiningCommonsMenuItem menu_item_2 =
        UCSBDiningCommonsMenuItem.builder()
            .dining_commons_code("carrillo")
            .name("Spaghetti")
            .station("Italian Plate")
            .build();

    ArrayList<UCSBDiningCommonsMenuItem> expectedItems = new ArrayList<>();
    expectedItems.addAll(Arrays.asList(menu_item_1, menu_item_2));
    when(ucsbDiningCommonsMenuItemRepository.findAll()).thenReturn(expectedItems);
    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/UCSBDiningCommonsMenuItem/all"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(ucsbDiningCommonsMenuItemRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedItems);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_menu_item() throws Exception {
    // arrange
    UCSBDiningCommonsMenuItem menu_item_1 =
        UCSBDiningCommonsMenuItem.builder()
            .dining_commons_code("ortega")
            .name("MacnCheese")
            .station("East_Plate")
            .build();
    when(ucsbDiningCommonsMenuItemRepository.save(eq(menu_item_1))).thenReturn(menu_item_1);
    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/UCSBDiningCommonsMenuItem/post?dining_commons_code=ortega&name=MacnCheese&station=East_Plate")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(ucsbDiningCommonsMenuItemRepository, times(1)).save(menu_item_1);
    String expectedJson = mapper.writeValueAsString(menu_item_1);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_edit_an_existing_menu_item() throws Exception {
    // arrange
    UCSBDiningCommonsMenuItem menu_item_Orig =
        UCSBDiningCommonsMenuItem.builder()
            .dining_commons_code("ortega")
            .name("Pizza")
            .station("Blue Plate")
            .build();

    UCSBDiningCommonsMenuItem menu_item_Edited =
        UCSBDiningCommonsMenuItem.builder()
            .dining_commons_code("carrillo")
            .name("pizza")
            .station("Blue Plate")
            .build();

    String requestBody = mapper.writeValueAsString(menu_item_Edited);

    when(ucsbDiningCommonsMenuItemRepository.findById(eq(67L)))
        .thenReturn(Optional.of(menu_item_Orig));

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/UCSBDiningCommonsMenuItem?id=67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(ucsbDiningCommonsMenuItemRepository, times(1)).findById(67L);
    verify(ucsbDiningCommonsMenuItemRepository, times(1))
        .save(menu_item_Edited); // should be saved with correct user
    String responseString = response.getResponse().getContentAsString();
    assertEquals(requestBody, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_edit_menu_item_that_does_not_exist() throws Exception {
    // arrange
    UCSBDiningCommonsMenuItem edited_menu_item =
        UCSBDiningCommonsMenuItem.builder()
            .dining_commons_code("portola")
            .name("Mac-n-Cheese")
            .station("West Plate")
            .build();

    String requestBody = mapper.writeValueAsString(edited_menu_item);

    when(ucsbDiningCommonsMenuItemRepository.findById(eq(67L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/UCSBDiningCommonsMenuItem?id=67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(ucsbDiningCommonsMenuItemRepository, times(1)).findById(67L);
    Map<String, Object> json = responseToJson(response);
    assertEquals("UCSBDiningCommonsMenuItem with id 67 not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_edit_all_fields_of_existing_menu_item() throws Exception {
    // arrange
    UCSBDiningCommonsMenuItem menu_item_Orig =
        UCSBDiningCommonsMenuItem.builder()
            .dining_commons_code("ortega")
            .name("Pizza")
            .station("Blue Plate")
            .build();

    UCSBDiningCommonsMenuItem menu_item_Edited =
        UCSBDiningCommonsMenuItem.builder()
            .dining_commons_code("carrillo") // changed
            .name("Pasta") // changed
            .station("Red Plate") // changed
            .build();

    String requestBody = mapper.writeValueAsString(menu_item_Edited);

    when(ucsbDiningCommonsMenuItemRepository.findById(eq(67L)))
        .thenReturn(Optional.of(menu_item_Orig));

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/UCSBDiningCommonsMenuItem?id=67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(ucsbDiningCommonsMenuItemRepository, times(1)).findById(67L);
    verify(ucsbDiningCommonsMenuItemRepository, times(1)).save(menu_item_Edited);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(requestBody, responseString);
  }
}
