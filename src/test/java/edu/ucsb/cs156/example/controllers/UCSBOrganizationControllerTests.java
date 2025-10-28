package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.UCSBOrganization;
import edu.ucsb.cs156.example.repositories.UCSBOrganizationRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = UCSBOrganizationController.class)
@Import(TestConfig.class)
public class UCSBOrganizationControllerTests extends ControllerTestCase {

  @MockBean UCSBOrganizationRepository ucsbOrganizationRepository;
  @MockBean UserRepository userRepository;

  // --- Authorization Tests ---

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

  // --- GET /all ---

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

  // --- POST /post ---

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
}
