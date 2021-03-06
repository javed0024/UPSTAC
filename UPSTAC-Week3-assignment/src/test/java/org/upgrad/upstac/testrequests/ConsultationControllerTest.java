package org.upgrad.upstac.testrequests;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.web.server.ResponseStatusException;
import org.upgrad.upstac.testrequests.TestRequest;
import org.upgrad.upstac.testrequests.consultation.ConsultationController;
import org.upgrad.upstac.testrequests.consultation.CreateConsultationRequest;
import org.upgrad.upstac.testrequests.consultation.DoctorSuggestion;
import org.upgrad.upstac.testrequests.lab.TestStatus;
import org.upgrad.upstac.testrequests.RequestStatus;
import org.upgrad.upstac.testrequests.TestRequestQueryService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Slf4j
class ConsultationControllerTest {
    @Autowired
    ConsultationController consultationController;

    @Autowired
    TestRequestQueryService testRequestQueryService;

    /**
     * Testing assignForConsultation request with valid test request ID
     * It should update the request status
     */
    @Test
    @WithUserDetails(value = "doctor")
    public void calling_assignForConsultation_with_valid_test_request_id_should_update_the_request_status() {
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_COMPLETED);
        TestRequest updatedTestRequest = consultationController.assignForConsultation(testRequest.getRequestId());
        assertThat(updatedTestRequest.getRequestId(), equalTo(testRequest.getRequestId()));
        assertThat(updatedTestRequest.getStatus(), equalTo(RequestStatus.DIAGNOSIS_IN_PROCESS));
        assertNotNull(updatedTestRequest.getConsultation());
    }

    public TestRequest getTestRequestByStatus(RequestStatus status) {
        return testRequestQueryService.findBy(status).stream().findFirst().get();
    }

    /**
     * Testing assignForConsultation request with invalid test request ID
     * It should through an exception
     */
    @Test
    @WithUserDetails(value = "doctor")
    public void calling_assignForConsultation_with_invalid_test_request_id_should_throw_exception() {
        Long InvalidRequestId = -34L;

        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, () -> {
            consultationController.assignForConsultation(InvalidRequestId);
        });

        assertThat(responseStatusException.getMessage(), containsString("Invalid ID"));
    }

    /**
     * Testing updateConsultation with valid test request ID
     * It should update the request status and update consultation details
     */
    @Test
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_valid_test_request_id_should_update_the_request_status_and_update_consultation_details() {
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
        CreateConsultationRequest createConsultationRequest = getCreateConsultationRequest(testRequest);
        TestRequest updatedTestRequest = consultationController.updateConsultation(testRequest.getRequestId(), createConsultationRequest);
        assertThat(updatedTestRequest.getRequestId(), equalTo(testRequest.getRequestId()));
        assertThat(updatedTestRequest.getStatus(), equalTo(RequestStatus.COMPLETED));
        assertThat(updatedTestRequest.getConsultation().getSuggestion(), equalTo(createConsultationRequest.getSuggestion()));
    }

    /**
     * Testing updateConsultation with invalid test request ID
     * It should through an exception
     */
    @Test
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_invalid_test_request_id_should_throw_exception() {
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
        CreateConsultationRequest createConsultationRequest = getCreateConsultationRequest(testRequest);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            consultationController.updateConsultation(-98L, createConsultationRequest);
        });

        assertThat(exception.getMessage(), containsString("Invalid ID"));
    }

    /**
     * Testing updateConsultation request with invalid empty status
     * It should through an exception
     */
    @Test
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_invalid_empty_status_should_throw_exception() {
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
        CreateConsultationRequest createConsultationRequest = getCreateConsultationRequest(testRequest);
        createConsultationRequest.setSuggestion(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            consultationController.updateConsultation(testRequest.getRequestId(), createConsultationRequest);
        });

        assertThat(exception.getMessage(), containsString("ConstraintViolationException"));
    }

    /**
     * Testing getCreateConsultationRequest
     * It should update the doctor suggestion based on lab result
     */
    public CreateConsultationRequest getCreateConsultationRequest(TestRequest testRequest) {
        CreateConsultationRequest createConsultationRequest = new CreateConsultationRequest();
        if (testRequest.getLabResult().getResult().equals(TestStatus.POSITIVE)) {
            createConsultationRequest.setComments("looks ok, suggest to take medicines at home");
            createConsultationRequest.setSuggestion(DoctorSuggestion.HOME_QUARANTINE);
        } else {
            createConsultationRequest.setComments("ok");
            createConsultationRequest.setSuggestion(DoctorSuggestion.NO_ISSUES);
        }
        return createConsultationRequest;
    }
}