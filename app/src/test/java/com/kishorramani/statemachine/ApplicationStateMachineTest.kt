package com.kishorramani.statemachine

import com.kishorramani.statemachine.domain.model.ApplicationEvent
import com.kishorramani.statemachine.domain.model.ApplicationState
import com.kishorramani.statemachine.domain.model.TransitionResult
import com.kishorramani.statemachine.domain.state.ApplicationStateMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApplicationStateMachineTest {

    private lateinit var stateMachine: ApplicationStateMachine

    @Before
    fun setUp() {
        stateMachine = ApplicationStateMachine()
    }

    @Test
    fun `draft can transition to submitted`() {
        val result = stateMachine.evaluateTransition(ApplicationState.Draft, ApplicationEvent.Submit)
        assertTrue(result is TransitionResult.Success)
        assertEquals(ApplicationState.Submitted, (result as TransitionResult.Success).state)
    }

    @Test
    fun `submitted can transition to document verification`() {
        val result = stateMachine.evaluateTransition(ApplicationState.Submitted, ApplicationEvent.StartVerification)
        assertTrue(result is TransitionResult.Success)
        assertEquals(ApplicationState.DocumentVerification, (result as TransitionResult.Success).state)
    }

    @Test
    fun `document verification can pass to credit checking`() {
        val result = stateMachine.evaluateTransition(ApplicationState.DocumentVerification, ApplicationEvent.DocumentsVerified)
        assertTrue(result is TransitionResult.Success)
        assertEquals(ApplicationState.CreditChecking, (result as TransitionResult.Success).state)
    }

    @Test
    fun `document verification can fail to verification failed`() {
        val result = stateMachine.evaluateTransition(ApplicationState.DocumentVerification, ApplicationEvent.DocumentsRejected)
        assertTrue(result is TransitionResult.Success)
        assertEquals(ApplicationState.VerificationFailed, (result as TransitionResult.Success).state)
    }

    @Test
    fun `credit checking can approve to approved`() {
        val result = stateMachine.evaluateTransition(ApplicationState.CreditChecking, ApplicationEvent.CreditApproved)
        assertTrue(result is TransitionResult.Success)
        assertEquals(ApplicationState.Approved, (result as TransitionResult.Success).state)
    }

    @Test
    fun `credit checking can reject to rejected`() {
        val result = stateMachine.evaluateTransition(ApplicationState.CreditChecking, ApplicationEvent.CreditRejected)
        assertTrue(result is TransitionResult.Success)
        assertEquals(ApplicationState.Rejected, (result as TransitionResult.Success).state)
    }

    @Test
    fun `approved can start payment to payment pending`() {
        val result = stateMachine.evaluateTransition(ApplicationState.Approved, ApplicationEvent.StartPayment)
        assertTrue(result is TransitionResult.Success)
        assertEquals(ApplicationState.PaymentPending, (result as TransitionResult.Success).state)
    }

    @Test
    fun `payment pending can transition to processing`() {
        val result = stateMachine.evaluateTransition(ApplicationState.PaymentPending, ApplicationEvent.PaymentProcessing)
        assertTrue(result is TransitionResult.Success)
        assertEquals(ApplicationState.PaymentProcessing, (result as TransitionResult.Success).state)
    }

    @Test
    fun `payment processing can succeed to payment success`() {
        val result = stateMachine.evaluateTransition(ApplicationState.PaymentProcessing, ApplicationEvent.PaymentSuccess)
        assertTrue(result is TransitionResult.Success)
        assertEquals(ApplicationState.PaymentSuccess, (result as TransitionResult.Success).state)
    }

    @Test
    fun `payment processing can fail to payment failed`() {
        val result = stateMachine.evaluateTransition(ApplicationState.PaymentProcessing, ApplicationEvent.PaymentFailed)
        assertTrue(result is TransitionResult.Success)
        assertEquals(ApplicationState.PaymentFailed, (result as TransitionResult.Success).state)
    }

    @Test
    fun `payment failed can retry to payment processing`() {
        val result = stateMachine.evaluateTransition(ApplicationState.PaymentFailed, ApplicationEvent.RetryPayment)
        assertTrue(result is TransitionResult.Success)
        assertEquals(ApplicationState.PaymentProcessing, (result as TransitionResult.Success).state)
    }

    @Test
    fun `payment success can complete`() {
        val result = stateMachine.evaluateTransition(ApplicationState.PaymentSuccess, ApplicationEvent.Complete)
        assertTrue(result is TransitionResult.Success)
        assertEquals(ApplicationState.Completed, (result as TransitionResult.Success).state)
    }

    // INVALID TRANSITIONS

    @Test
    fun `payment success event is invalid from draft`() {
        val result = stateMachine.evaluateTransition(ApplicationState.Draft, ApplicationEvent.PaymentSuccess)
        assertTrue(result is TransitionResult.Invalid)
        assertEquals(ApplicationState.Draft, (result as TransitionResult.Invalid).currentState)
    }

    @Test
    fun `rejected application cannot start payment`() {
        val result = stateMachine.evaluateTransition(ApplicationState.Rejected, ApplicationEvent.StartPayment)
        assertTrue(result is TransitionResult.Invalid)
    }

    @Test
    fun `completed application cannot be modified`() {
        val result = stateMachine.evaluateTransition(ApplicationState.Completed, ApplicationEvent.StartPayment)
        assertTrue(result is TransitionResult.Invalid)
    }

    @Test
    fun `payment success cannot transition to payment failed`() {
        val result = stateMachine.evaluateTransition(ApplicationState.PaymentSuccess, ApplicationEvent.PaymentFailed)
        assertTrue(result is TransitionResult.Invalid)
    }

    @Test
    fun `credit checking cannot directly complete`() {
        val result = stateMachine.evaluateTransition(ApplicationState.CreditChecking, ApplicationEvent.Complete)
        assertTrue(result is TransitionResult.Invalid)
    }
}
