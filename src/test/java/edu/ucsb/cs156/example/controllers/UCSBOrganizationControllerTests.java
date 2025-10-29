package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.UCSBOrganization;
import edu.ucsb.cs156.example.repositories.UCSBOrganizationRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = UCSBOrganizationController.class)
@Import(TestConfig.class)
public class UCSBOrganizationControllerTests extends ControllerTestCase {

  @MockBean UCSBOrganizationRepository ucsbOrganizationRepository;
  @MockBean UserRepository userRepository;

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc.perform(get("/api/ucsborganizations/all")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all_status_ok() throws Exception {
    mockMvc.perform(get("/api/ucsborganizations/all")).andExpect(status().is(200));
  }

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc.perform(post("/api/ucsborganizations/post")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc.perform(post("/api/ucsborganizations/post")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_organizations() throws Exception {
    UCSBOrganization a = new UCSBOrganization();
    a.setOrgCode("ZPR");
    a.setOrgTranslationShort("ZETA PHI RHO");
    a.setOrgTranslation("ZETA PHI RHO");
    a.setInactive(false);

    UCSBOrganization b = new UCSBOrganization();
    b.setOrgCode("OSLI");
    b.setOrgTranslationShort("STUDENT LIFE");
    b.setOrgTranslation("OFFICE OF STUDENT LIFE");
    b.setInactive(false);

    when(ucsbOrganizationRepository.findAll()).thenReturn(new ArrayList<>(Arrays.asList(a, b)));

    MvcResult response =
        mockMvc.perform(get("/api/ucsborganizations/all")).andExpect(status().isOk()).andReturn();

    verify(ucsbOrganizationRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(Arrays.asList(a, b));
    assertEquals(expectedJson, response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_post_new_organization_and_all_fields_are_set() throws Exception {
    when(ucsbOrganizationRepository.save(any(UCSBOrganization.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    MvcResult res =
        mockMvc
            .perform(
                post("/api/ucsborganizations/post")
                    .param("orgCode", "OSLI")
                    .param("orgTranslationShort", "STUDENT LIFE")
                    .param("orgTranslation", "OFFICE OF STUDENT LIFE")
                    .param("inactive", "false")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    ArgumentCaptor<UCSBOrganization> captor = ArgumentCaptor.forClass(UCSBOrganization.class);
    verify(ucsbOrganizationRepository, times(1)).save(captor.capture());
    UCSBOrganization saved = captor.getValue();

    assertEquals("OSLI", saved.getOrgCode());
    assertEquals("STUDENT LIFE", saved.getOrgTranslationShort());
    assertEquals("OFFICE OF STUDENT LIFE", saved.getOrgTranslation());
    assertFalse(saved.getInactive());

    String expectedJson = mapper.writeValueAsString(saved);
    assertEquals(expectedJson, res.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_post_sets_inactive_true_when_requested() throws Exception {
    when(ucsbOrganizationRepository.save(any(UCSBOrganization.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    MvcResult res =
        mockMvc
            .perform(
                post("/api/ucsborganizations/post")
                    .param("orgCode", "SKY")
                    .param("orgTranslationShort", "SKYDIVING CLUB")
                    .param("orgTranslation", "SKYDIVING CLUB AT UCSB")
                    .param("inactive", "true")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    ArgumentCaptor<UCSBOrganization> captor = ArgumentCaptor.forClass(UCSBOrganization.class);
    verify(ucsbOrganizationRepository, times(1)).save(captor.capture());
    UCSBOrganization saved = captor.getValue();

    assertEquals("SKY", saved.getOrgCode());
    assertEquals("SKYDIVING CLUB", saved.getOrgTranslationShort());
    assertEquals("SKYDIVING CLUB AT UCSB", saved.getOrgTranslation());
    assertEquals(true, saved.getInactive());

    String expectedJson = mapper.writeValueAsString(saved);
    assertEquals(expectedJson, res.getResponse().getContentAsString());
  }

  // Get addtions

  @Test
  public void logged_out_users_cannot_get_by_orgCode() throws Exception {
    mockMvc
        .perform(get("/api/ucsborganizations").param("orgCode", "ZPR"))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_by_orgCode_when_it_exists() throws Exception {
    UCSBOrganization expected =
        UCSBOrganization.builder()
            .orgCode("ZPR")
            .orgTranslationShort("ZETA PHI RHO")
            .orgTranslation("ZETA PHI RHO")
            .inactive(false)
            .build();

    when(ucsbOrganizationRepository.findById(eq("ZPR"))).thenReturn(Optional.of(expected));
    MvcResult response =
        mockMvc
            .perform(get("/api/ucsborganizations").param("orgCode", "ZPR"))
            .andExpect(status().isOk())
            .andReturn();
    verify(ucsbOrganizationRepository, times(1)).findById(eq("ZPR"));
    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(expected);
    assertEquals(expectedJson, responseString);
    UCSBOrganization passed = mapper.readValue(responseString, UCSBOrganization.class);
    assertEquals("ZPR", passed.getOrgCode());
    assertEquals("ZETA PHI RHO", passed.getOrgTranslationShort());
    assertEquals("ZETA PHI RHO", passed.getOrgTranslation());
    assertFalse(passed.getInactive());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_gets_404_when_orgCode_not_found() throws Exception {
    when(ucsbOrganizationRepository.findById(eq("NOPE"))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(get("/api/ucsborganizations").param("orgCode", "NOPE"))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(ucsbOrganizationRepository, times(1)).findById(eq("NOPE"));

    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("UCSBOrganization with id NOPE not found", json.get("message"));
  }

  // put endpoint
  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_edit_existing_organization() throws Exception {
    UCSBOrganization existing =
        UCSBOrganization.builder()
            .orgCode("ZPR")
            .orgTranslationShort("ZETA PHI RHO")
            .orgTranslation("ZETA PHI RHO")
            .inactive(false)
            .build();

    UCSBOrganization edited =
        UCSBOrganization.builder()
            .orgCode("ZPR")
            .orgTranslationShort("ZETA PHI RHO FRATERNITY")
            .orgTranslation("ZETA PHI RHO INC.")
            .inactive(true)
            .build();

    String requestBody = mapper.writeValueAsString(edited);

    when(ucsbOrganizationRepository.findById(eq("ZPR"))).thenReturn(Optional.of(existing));
    when(ucsbOrganizationRepository.save(any(UCSBOrganization.class))).thenReturn(edited);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/ucsborganizations")
                    .param("orgCode", "ZPR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(ucsbOrganizationRepository, times(1)).findById("ZPR");
    verify(ucsbOrganizationRepository, times(1)).save(any(UCSBOrganization.class));
    String expectedJson = mapper.writeValueAsString(edited);
    assertEquals(expectedJson, response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_edit_nonexistent_organization() throws Exception {
    UCSBOrganization edited =
        UCSBOrganization.builder()
            .orgCode("NOPE")
            .orgTranslationShort("DOES NOT EXIST")
            .orgTranslation("DOES NOT EXIST")
            .inactive(true)
            .build();

    String requestBody = mapper.writeValueAsString(edited);

    when(ucsbOrganizationRepository.findById(eq("NOPE"))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/ucsborganizations")
                    .param("orgCode", "NOPE")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(ucsbOrganizationRepository, times(1)).findById("NOPE");
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("UCSBOrganization with id NOPE not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_edit_existing_org_and_fields_are_updated() throws Exception {
    UCSBOrganization original =
        UCSBOrganization.builder()
            .orgCode("ZPR")
            .orgTranslationShort("ZETA PHI RHO")
            .orgTranslation("ZETA PHI RHO")
            .inactive(false)
            .build();

    UCSBOrganization edited =
        UCSBOrganization.builder()
            .orgCode("ZPR")
            .orgTranslationShort("ZETA PHI RHO (UPDATED)")
            .orgTranslation("ZETA PHI RHO UPDATED")
            .inactive(true)
            .build();

    String requestBody = mapper.writeValueAsString(edited);

    when(ucsbOrganizationRepository.findById(eq("ZPR"))).thenReturn(Optional.of(original));
    when(ucsbOrganizationRepository.save(any(UCSBOrganization.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/ucsborganizations")
                    .param("orgCode", "ZPR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(ucsbOrganizationRepository, times(1)).findById(eq("ZPR"));
    ArgumentCaptor<UCSBOrganization> captor = ArgumentCaptor.forClass(UCSBOrganization.class);
    verify(ucsbOrganizationRepository, times(1)).save(captor.capture());
    UCSBOrganization saved = captor.getValue();

    assertEquals("ZPR", saved.getOrgCode());
    assertEquals("ZETA PHI RHO (UPDATED)", saved.getOrgTranslationShort());
    assertEquals("ZETA PHI RHO UPDATED", saved.getOrgTranslation());
    assertTrue(saved.getInactive());

    assertEquals(requestBody, response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_put_org_returns_404_when_not_found() throws Exception {
    UCSBOrganization edited =
        UCSBOrganization.builder()
            .orgCode("NOPE")
            .orgTranslationShort("X")
            .orgTranslation("Y")
            .inactive(true)
            .build();

    String requestBody = mapper.writeValueAsString(edited);

    when(ucsbOrganizationRepository.findById(eq("NOPE"))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/ucsborganizations")
                    .param("orgCode", "NOPE")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(ucsbOrganizationRepository, times(1)).findById(eq("NOPE"));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("UCSBOrganization with id NOPE not found", json.get("message"));
  }

  // delete tests
  @Test
  public void logged_out_users_cannot_delete() throws Exception {
    mockMvc
        .perform(delete("/api/ucsborganizations").param("orgCode", "OSLI"))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_delete() throws Exception {
    mockMvc
        .perform(delete("/api/ucsborganizations").param("orgCode", "OSLI"))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_delete_an_organization() throws Exception {
    UCSBOrganization osli =
        UCSBOrganization.builder()
            .orgCode("OSLI")
            .orgTranslationShort("STUDENT LIFE")
            .orgTranslation("OFFICE OF STUDENT LIFE")
            .inactive(false)
            .build();
    when(ucsbOrganizationRepository.findById(eq("OSLI"))).thenReturn(Optional.of(osli));
    MvcResult response =
        mockMvc
            .perform(delete("/api/ucsborganizations").param("orgCode", "OSLI").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();
    verify(ucsbOrganizationRepository, times(1)).findById("OSLI");
    verify(ucsbOrganizationRepository, times(1)).delete(any());
    Map<String, Object> json = responseToJson(response);
    assertEquals("UCSBOrganization with id OSLI deleted", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_tries_to_delete_nonexistent_org_and_gets_404() throws Exception {
    when(ucsbOrganizationRepository.findById(eq("NOPE"))).thenReturn(Optional.empty());
    MvcResult response =
        mockMvc
            .perform(delete("/api/ucsborganizations").param("orgCode", "NOPE").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(ucsbOrganizationRepository, times(1)).findById("NOPE");
    Map<String, Object> json = responseToJson(response);
    assertEquals("UCSBOrganization with id NOPE not found", json.get("message"));
  }
}
