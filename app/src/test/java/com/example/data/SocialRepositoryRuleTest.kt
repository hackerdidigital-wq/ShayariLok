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

  @Test
  fun updateMyProfile_savesBioCityAddress_andSearchByEmailHandleFindsRealUser() =
    runBlocking {
      val aliceUid = signInTestUser(ALICE_EMAIL)
      val aliceRepo = SocialRepository(firestore)
      val aliceEmailHandle = SocialRepository.buildHandleFromEmail(ALICE_EMAIL, aliceUid)
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo.syncGoogleUserOnLogin(aliceEmailHandle, "Alice Shayar", ALICE_EMAIL, "").getOrThrow()
      }

      val updateRes =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          aliceRepo.updateMyProfile(
            displayName = "Alice Mirza",
            handle = aliceEmailHandle,
            city = "Lucknow",
            address = "Hazratganj, Lucknow",
            bio = "Kalam aur Ishq ka safar",
          )
        }
      assertTrue("Expected profile update to succeed, got: ${updateRes.exceptionOrNull()}", updateRes.isSuccess)
      val updated =
        withTimeout(FLOW_TIMEOUT_MS) {
          aliceRepo.observeMyProfile(aliceUid).first {
            it != null && it.city == "Lucknow" && it.address == "Hazratganj, Lucknow"
          }!!
        }
      assertEquals("Lucknow", updated.city)
      assertEquals("Hazratganj, Lucknow", updated.address)
      assertEquals("Kalam aur Ishq ka safar", updated.bio)

      // Second user Bob searches for Alice by her @SL_XXXX email handle across devices
      val bobUid = signInTestUser(BOB_EMAIL)
      val bobRepo = SocialRepository(firestore)
      bobRepo.syncGoogleUserOnLogin(
        SocialRepository.buildHandleFromEmail(BOB_EMAIL, bobUid),
        "Bob",
        BOB_EMAIL,
        "",
      ).getOrThrow()

      val searchRes =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          bobRepo.searchDiscoverableProfiles(aliceEmailHandle)
        }
      assertTrue("Expected search to succeed, got: ${searchRes.exceptionOrNull()}", searchRes.isSuccess)
      val matches = searchRes.getOrThrow()
      assertTrue(
        "Expected search for $aliceEmailHandle to return Alice Mirza with city/bio",
        matches.any { profile ->
          profile.effectiveUid == aliceUid &&
            profile.city == "Lucknow" &&
            profile.bio == "Kalam aur Ishq ka safar"
        },
      )
    }

  private companion object {
    const val ALICE_EMAIL = "alice_shayar@test.com"
    const val BOB_EMAIL = "bob_shayar@test.com"
    const val EVE_EMAIL = "eve_intruder@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3500L
  }
}
