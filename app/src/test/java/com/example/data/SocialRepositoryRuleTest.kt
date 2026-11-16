package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SocialRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun syncGoogleUserAndObserveDirectory_authenticatedUser_succeedsWithoutPermissionDenied() =
    runBlocking {
      signInTestUser(ALICE_EMAIL)
      val repository = SocialRepository(firestore)

      val syncResult =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repository.syncGoogleUserOnLogin(
            defaultHandle = "@SL_1001",
            defaultDisplayName = "Alice Shayar",
            email = ALICE_EMAIL,
            photoUrl = "",
          )
        }
      assertTrue("Expected login sync to succeed, got: ${syncResult.exceptionOrNull()}", syncResult.isSuccess)

      val profiles =
        withTimeout(FLOW_TIMEOUT_MS) {
          repository.observeDiscoverableProfiles().first { it.isNotEmpty() }
        }
      assertTrue(profiles.any { it.displayName == "Alice Shayar" })
    }

  @Test
  fun sendChatMessage_andObserveMessages_enforcesPrivate1on1Access() =
    runBlocking {
      val aliceUid = signInTestUser(ALICE_EMAIL)
      val aliceRepo = SocialRepository(firestore)
      val aliceProfile =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          aliceRepo.syncGoogleUserOnLogin("@SL_2001", "Alice", ALICE_EMAIL, "").getOrThrow()
        }

      val bobUid = signInTestUser(BOB_EMAIL)
      val bobRepo = SocialRepository(firestore)
      val bobProfile =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          bobRepo.syncGoogleUserOnLogin("@SL_2002", "Bob", BOB_EMAIL, "").getOrThrow()
        }

      // Alice opens chat and sends a message to Bob
      signInTestUser(ALICE_EMAIL)
      val roomId = SocialRepository.buildChatRoomId(aliceUid, bobUid)
      val sendRes =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          aliceRepo.sendChatMessage(aliceProfile, bobProfile, "Shukriya Dost")
        }
      assertTrue("Expected chat send to succeed, got: ${sendRes.exceptionOrNull()}", sendRes.isSuccess)

      // Bob reads the private chat room
      signInTestUser(BOB_EMAIL)
      val bobMessages =
        withTimeout(FLOW_TIMEOUT_MS) {
          bobRepo.observeChatMessages(roomId).first { it.isNotEmpty() }
        }
      assertEquals("Shukriya Dost", bobMessages.first().text)

      // Third user Eve must be blocked from reading Alice & Bob's room
      signInTestUser(EVE_EMAIL)
      val eveRepo = SocialRepository(firestore)
      try {
        withTimeout(FLOW_TIMEOUT_MS) { eveRepo.observeChatMessages(roomId).first() }
        fail("Expected non-participant Eve to be denied access")
      } catch (e: Exception) {
        val fsEx = e.unwrapFirestoreException()
        assertTrue(
          "Expected PERMISSION_DENIED or SecurityException, got $e",
          fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED || e is SecurityException,
        )
      }
    }

  @Test
  fun observeDiscoverableProfiles_unauthenticated_failsWithPermissionDenied() =
    runBlocking {
      auth.signOut()
      val repository = SocialRepository(firestore)
      try {
        withTimeout(FLOW_TIMEOUT_MS) { repository.observeDiscoverableProfiles().first() }
        fail("Expected unauthenticated directory read to fail")
      } catch (e: Exception) {
        val fsEx = e.unwrapFirestoreException()
        assertTrue(
          "Expected PERMISSION_DENIED or IllegalStateException, got $e",
          fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED || e is IllegalStateException,
        )
      }
    }

  private companion object {
    const val ALICE_EMAIL = "alice_shayar@test.com"
    const val BOB_EMAIL = "bob_shayar@test.com"
    const val EVE_EMAIL = "eve_intruder@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3500L
  }
}
