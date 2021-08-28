package org.upgrad.upstac.testrequests.consultation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.upgrad.upstac.config.security.UserLoggedInService;
import org.upgrad.upstac.exception.AppException;
import org.upgrad.upstac.testrequests.RequestStatus;
import org.upgrad.upstac.testrequests.TestRequest;
import org.upgrad.upstac.testrequests.TestRequestQueryService;
import org.upgrad.upstac.testrequests.TestRequestUpdateService;
import org.upgrad.upstac.testrequests.flow.TestRequestFlowService;
import org.upgrad.upstac.users.User;

import javax.validation.ConstraintViolationException;
import java.util.List;

import static org.upgrad.upstac.exception.UpgradResponseStatusException.asBadRequest;
import static org.upgrad.upstac.exception.UpgradResponseStatusException.asConstraintViolation;

@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {

    Logger log = LoggerFactory.getLogger(ConsultationController.class);

    @Autowired
    private TestRequestUpdateService testRequestUpdateService;

    @Autowired
    private TestRequestQueryService testRequestQueryService;

    @Autowired
    TestRequestFlowService testRequestFlowService;

    @Autowired
    private UserLoggedInService userLoggedInService;

    /**
     * Returns the list of test requests for Consultation,
     * having status as 'LAB_TEST_COMPLETED'
     */
    @GetMapping("/in-queue")
    @PreAuthorize("hasAnyRole('DOCTOR')")
    public List<TestRequest> getForConsultations() {
        return testRequestQueryService.findBy(RequestStatus.LAB_TEST_COMPLETED);
    }

    /**
     * Returns the list of test requests
     * assigned to current doctor
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR')")
    public List<TestRequest> getForDoctor() {
        User user = userLoggedInService.getLoggedInUser();
        return testRequestQueryService.findByDoctor(user);
    }

    /**
     * Assigns a particular test request
     * to the current doctor
     */
    @PreAuthorize("hasAnyRole('DOCTOR')")
    @PutMapping("/assign/{id}")
    public TestRequest assignForConsultation(@PathVariable Long id) {
        try {
            User user = userLoggedInService.getLoggedInUser();
            TestRequest result = testRequestUpdateService.assignForConsultation(id, user);
            return result;
        } catch (AppException e) {
            throw asBadRequest(e.getMessage());
        }
    }

    /**
     * Updates the result of the current test request id
     * with test doctor comments
     */
    @PreAuthorize("hasAnyRole('DOCTOR')")
    @PutMapping("/update/{id}")
    public TestRequest updateConsultation(@PathVariable Long id, @RequestBody CreateConsultationRequest testResult) {
        try {
            User tester = userLoggedInService.getLoggedInUser();
            TestRequest result = testRequestUpdateService.updateConsultation(id, testResult, tester);
            return result;
        } catch (ConstraintViolationException e) {
            throw asConstraintViolation(e);
        } catch (AppException e) {
            throw asBadRequest(e.getMessage());
        }
    }
}
