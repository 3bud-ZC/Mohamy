package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.Client
import com.example.data.LicenseActivationPayload
import com.example.data.Repository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WorkspaceIsolationTest {

  @Test
  fun `each account keeps isolated local clients workspace`() = runTest {
    val application = ApplicationProvider.getApplicationContext<Application>()
    cleanupWorkspace(application)

    try {
      val accountOne = "lawyer_one"
      val accountTwo = "lawyer_two"

      val repoAccountOne = Repository(AppDatabase.getDatabase(application), application)
      repoAccountOne.persistActivatedLicense(payload(accountOne))
      repoAccountOne.clientDao.insertClient(Client(name = "عميل الحساب الأول", phone = "01000000001"))
      repoAccountOne.archiveActiveWorkspaceAndLogout()

      repoAccountOne.switchToAccountWorkspace(accountTwo)
      AppDatabase.closeInstance()

      val repoAccountTwo = Repository(AppDatabase.getDatabase(application), application)
      repoAccountTwo.persistActivatedLicense(payload(accountTwo))
      val firstLoginClientsForSecondAccount = repoAccountTwo.clientDao.getAllClients().first()
      assertTrue(firstLoginClientsForSecondAccount.isEmpty())

      repoAccountTwo.clientDao.insertClient(Client(name = "عميل الحساب الثاني", phone = "01000000002"))
      repoAccountTwo.archiveActiveWorkspaceAndLogout()

      repoAccountTwo.switchToAccountWorkspace(accountOne)
      AppDatabase.closeInstance()

      val repoAccountOneAgain = Repository(AppDatabase.getDatabase(application), application)
      repoAccountOneAgain.persistActivatedLicense(payload(accountOne))
      val clientsForFirstAccount = repoAccountOneAgain.clientDao.getAllClients().first().map { it.name }
      assertEquals(listOf("عميل الحساب الأول"), clientsForFirstAccount)

      repoAccountOneAgain.archiveActiveWorkspaceAndLogout()
      repoAccountOneAgain.switchToAccountWorkspace(accountTwo)
      AppDatabase.closeInstance()

      val repoAccountTwoAgain = Repository(AppDatabase.getDatabase(application), application)
      repoAccountTwoAgain.persistActivatedLicense(payload(accountTwo))
      val clientsForSecondAccount = repoAccountTwoAgain.clientDao.getAllClients().first().map { it.name }
      assertEquals(listOf("عميل الحساب الثاني"), clientsForSecondAccount)
    } finally {
      cleanupWorkspace(application)
    }
  }

  private fun payload(username: String): LicenseActivationPayload {
    return LicenseActivationPayload(
      username = username,
      token = "token_$username",
      expiresAt = null,
      lawyerName = "محامي $username",
      officeName = "مكتب $username",
      phone = "01099999999",
      licenseKey = "LK-$username",
      deviceId = "device-$username"
    )
  }

  private fun cleanupWorkspace(application: Application) {
    AppDatabase.closeInstance()

    val dbFile = application.getDatabasePath(AppDatabase.DATABASE_NAME)
    application.deleteDatabase(AppDatabase.DATABASE_NAME)
    File(dbFile.path + "-wal").delete()
    File(dbFile.path + "-shm").delete()

    File(application.filesDir, "mohamy_phone").deleteRecursively()

    application
      .getSharedPreferences("mohamy_phone_prefs", Context.MODE_PRIVATE)
      .edit()
      .clear()
      .commit()
  }
}
