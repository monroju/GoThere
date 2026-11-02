package com.example.gothere.notify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** E13: topic names must match shared/alerts/alerts.py (`rules_<iso code>`). */
class RuleAlertTopicTest {
    @Test
    fun coveredDestinationsMapToIsoTopics() {
        assertEquals("rules_es", FcmTopicManager.ruleAlertTopic("spain"))
        assertEquals("rules_pt", FcmTopicManager.ruleAlertTopic("portugal"))
        assertEquals("rules_ie", FcmTopicManager.ruleAlertTopic("ireland"))
        assertEquals("rules_it", FcmTopicManager.ruleAlertTopic("italy"))
        assertEquals("rules_mx", FcmTopicManager.ruleAlertTopic("mexico"))
    }

    @Test
    fun uncoveredDestinationsHaveNoTopic() {
        assertNull(FcmTopicManager.ruleAlertTopic("canada"))
        assertNull(FcmTopicManager.ruleAlertTopic("uk_ancestry"))
    }
}
