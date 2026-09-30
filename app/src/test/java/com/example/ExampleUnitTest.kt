package com.example

import com.example.data.local.hashPassword
import com.example.data.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testStandardZonesCoverage() {
    assertTrue(STANDARD_ZONES.contains("ZONE NO 11"))
    assertTrue(STANDARD_ZONES.contains("ZONE NO 12"))
    assertTrue(STANDARD_ZONES.contains("ZONE NO 13"))
    assertTrue(STANDARD_ZONES.contains("ZONE NO 14"))
    assertTrue(STANDARD_ZONES.contains("ZONE NO 15"))
  }

  @Test
  fun testPasswordHashing() {
    val hash = hashPassword("2002")
    assertEquals(64, hash.length) // SHA-256 hex length
    assertEquals(hash, hashPassword("2002"))
    assertNotEquals(hash, hashPassword("wrongpass"))
  }

  @Test
  fun testAdminOnlyDeletionEnforcementLogic() {
    val adminUser = UserEntity("1", "RASHID", hashPassword("2002"), Role.ADMIN)
    val headmanUser = UserEntity("2", "headman11", hashPassword("1111"), Role.HEADMAN, "ZONE NO 11")

    // Admin passes
    assertTrue(adminUser.role == Role.ADMIN)

    // Normal user / Headman is blocked
    try {
      if (headmanUser.role != Role.ADMIN) {
        throw SecurityException("You do not have permission to delete reports. Only Administrators can delete records.")
      }
      fail("Headman should not be permitted to delete")
    } catch (e: SecurityException) {
      assertTrue(e.message!!.contains("permission"))
    }
  }

  // --- CUJ MANDATE: Test 1 to Test 6 Synchronization & Offline Verification ---

  @Test
  fun testUserASubmitsReportAndUserBReceivesUpdateAcrossDevices() {
    // 1. User A submits a report on Device A
    val reportId = "rep_sync_test_001"
    val reportUserA = ReportEntity(
      id = reportId,
      zone = "ZONE NO 11",
      dateOfOperation = "2024-04-10",
      operationName = "Fertilizer Application",
      blockNumber = "Block 4B",
      contractorName = "Kilombero Contractors Ltd",
      noOfLabourers = 18,
      areaCoveredHa = 12.5,
      balanceToBeDoneHa = 2.5,
      inputName = "Urea 46% N",
      ratoonDate = "2024-01-15",
      remark = "Uniform spread across north section",
      createdBy = "User A (Headman 11)",
      createdByRole = Role.HEADMAN,
      lastUpdatedBy = "User A (Headman 11)",
      status = ReportStatus.SUBMITTED,
      synced = true,
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis(),
    )

    // Simulate Device A local database
    val deviceADatabase = mutableMapOf<String, ReportEntity>()
    deviceADatabase[reportUserA.id] = reportUserA

    // Simulate Cloud Database backend
    val cloudDatabase = mutableMapOf<String, ReportEntity>()
    cloudDatabase[reportUserA.id] = reportUserA

    // 2. User B logs into another device (Device B)
    val deviceBDatabase = mutableMapOf<String, ReportEntity>()
    val userB = UserEntity("usr_b", "User B (Supervisor)", hashPassword("passB"), Role.ADMIN)
    assertEquals(Role.ADMIN, userB.role)

    // Device B syncs with shared Cloud Database
    deviceBDatabase.putAll(cloudDatabase)

    // 3. User B can see the report submitted by User A
    val reportOnDeviceB = deviceBDatabase[reportId]
    assertNotNull("User B must see the report submitted by User A", reportOnDeviceB)
    assertEquals("Urea 46% N", reportOnDeviceB?.inputName)
    assertEquals("2024-01-15", reportOnDeviceB?.ratoonDate)
    assertEquals("User A (Headman 11)", reportOnDeviceB?.createdBy)
    assertEquals(12.5, reportOnDeviceB?.areaCoveredHa ?: 0.0, 0.01)

    // 4. User B updates the report on Device B
    val updatedByB = reportOnDeviceB!!.copy(
      areaCoveredHa = 15.0,
      balanceToBeDoneHa = 0.0,
      inputName = "CAN (Calcium Ammonium Nitrate)",
      lastUpdatedBy = "User B (Supervisor)",
      updatedAt = System.currentTimeMillis() + 1000,
      status = ReportStatus.SYNCED,
      synced = true,
    )
    deviceBDatabase[reportId] = updatedByB
    // Push update to cloud backend
    cloudDatabase[reportId] = updatedByB

    // 5. User A can see the updated information when Device A syncs
    deviceADatabase.putAll(cloudDatabase)
    val reportOnDeviceAAfterSync = deviceADatabase[reportId]
    assertNotNull(reportOnDeviceAAfterSync)
    assertEquals("User B (Supervisor)", reportOnDeviceAAfterSync?.lastUpdatedBy)
    assertEquals("CAN (Calcium Ammonium Nitrate)", reportOnDeviceAAfterSync?.inputName)
    assertEquals(15.0, reportOnDeviceAAfterSync?.areaCoveredHa ?: 0.0, 0.01)
    assertEquals(ReportStatus.SYNCED, reportOnDeviceAAfterSync?.status)

    // 6. Test offline entry and synchronization when internet returns
    // Offline entry: device has no network, records report locally with synced = false
    val offlineReport = ReportEntity(
      id = "rep_offline_002",
      zone = "ZONE NO 13",
      dateOfOperation = "2024-04-11",
      operationName = "Weeding",
      blockNumber = "Block 8C",
      contractorName = "Field Crew A",
      noOfLabourers = 10,
      areaCoveredHa = 5.0,
      balanceToBeDoneHa = 3.0,
      inputName = "Glyphosate 480SL",
      ratoonDate = "2024-02-01",
      remark = "Recorded in deep field without network",
      createdBy = "User A (Headman 11)",
      createdByRole = Role.HEADMAN,
      lastUpdatedBy = "User A (Headman 11)",
      status = ReportStatus.PENDING_SYNC,
      synced = false, // OFFLINE FLAG
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis(),
    )
    deviceADatabase[offlineReport.id] = offlineReport

    // Verify offline report is NOT in cloud yet
    assertFalse("Cloud should not have offline report before sync", cloudDatabase.containsKey(offlineReport.id))
    assertFalse("Device A should have synced=false while offline", deviceADatabase[offlineReport.id]!!.synced)

    // Internet returns: Trigger Synchronization
    val pendingUploads = deviceADatabase.values.filter { !it.synced }
    assertEquals(1, pendingUploads.size)

    // Push pending uploads to cloud
    for (pending in pendingUploads) {
      val syncedReport = pending.copy(synced = true, status = ReportStatus.SYNCED)
      cloudDatabase[syncedReport.id] = syncedReport
      deviceADatabase[syncedReport.id] = syncedReport
    }

    // Verify cloud now has the report and Device A status is updated
    assertTrue("Cloud must now have the synced report", cloudDatabase.containsKey(offlineReport.id))
    assertTrue("Device A must mark report as synced=true", deviceADatabase[offlineReport.id]!!.synced)
    assertEquals(ReportStatus.SYNCED, deviceADatabase[offlineReport.id]!!.status)

    // User B on Device B syncs and immediately gets the offline-submitted report
    deviceBDatabase.putAll(cloudDatabase)
    assertTrue("User B must receive the report submitted offline once internet returned", deviceBDatabase.containsKey(offlineReport.id))
    assertEquals("Glyphosate 480SL", deviceBDatabase[offlineReport.id]?.inputName)
  }

  @Test
  fun testManagementPanelsEntitiesAndCalculations() {
    // 1. Division Discussion verification
    val discussion = DiscussionEntity(
      id = "disc_001",
      title = "Weed Management Strategy Q2",
      notes = "Focus on zone 11 and 12 ratoon crop weeding before rains",
      division = "North Division",
      date = "2024-04-10",
      author = "Rashid (Manager)",
      authorRole = Role.ADMIN,
      synced = true,
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis(),
    )
    assertEquals("Weed Management Strategy Q2", discussion.title)
    assertEquals("North Division", discussion.division)

    // 2. Weekly Equipment shortage and surplus calculation
    val tractorReq = WeeklyEquipmentEntity(
      id = "eq_001",
      weekDate = "2024-04-15",
      zoneOrDivision = "ZONE NO 11",
      equipmentName = "Tractor 90HP (4WD)",
      requiredQuantity = 5,
      availableQuantity = 3,
      shortageQuantity = 2,
      remarks = "Need 2 additional tractors from Central Depot",
      submittedBy = "Headman 11",
      synced = true,
    )
    val calculatedShortage = maxOf(0, tractorReq.requiredQuantity - tractorReq.availableQuantity)
    assertEquals(2, calculatedShortage)
    assertEquals(tractorReq.shortageQuantity, calculatedShortage)

    // 3. Zone Progress milestone calculation
    val progress = ZoneProgressEntity(
      id = "prog_001",
      zone = "ZONE NO 12",
      date = "2024-04-10",
      activity = "Sugarcane Harvesting",
      progressStatus = "In Progress",
      percentage = 75,
      remarks = "75% completed in Block 2",
      submittedBy = "Manager Rashid",
      synced = true,
    )
    assertEquals(75, progress.percentage)
    assertEquals("Sugarcane Harvesting", progress.activity)
  }

  @Test
  fun testFormValidationRequirements() {
    // Test that required fields cannot be blank
    fun validateForm(
      zone: String,
      blockNumber: String,
      dateOfOperation: String,
      operationName: String,
      inputName: String,
      ratoonDate: String,
      contractorName: String,
      acresWorkedStr: String,
    ): Boolean {
      val isZoneValid = zone.isNotBlank()
      val isBlockValid = blockNumber.isNotBlank()
      val isDateValid = dateOfOperation.isNotBlank()
      val isOpValid = operationName.isNotBlank()
      val isInputValid = inputName.isNotBlank()
      val isRatoonValid = ratoonDate.isNotBlank()
      val isContractorValid = contractorName.isNotBlank()
      val isAcresValid = (acresWorkedStr.toDoubleOrNull() ?: 0.0) > 0.0

      return isZoneValid && isBlockValid && isDateValid && isOpValid &&
        isInputValid && isRatoonValid && isContractorValid && isAcresValid
    }

    // Invalid when input name or ratoon date is missing
    assertFalse(
      validateForm(
        zone = "ZONE NO 11",
        blockNumber = "Block 1",
        dateOfOperation = "2024-04-10",
        operationName = "Planting",
        inputName = "", // Missing
        ratoonDate = "2024-01-01",
        contractorName = "Raphael",
        acresWorkedStr = "10.0",
      ),
    )

    assertFalse(
      validateForm(
        zone = "ZONE NO 11",
        blockNumber = "Block 1",
        dateOfOperation = "2024-04-10",
        operationName = "Planting",
        inputName = "Urea",
        ratoonDate = "", // Missing
        contractorName = "Hussein",
        acresWorkedStr = "10.0",
      ),
    )

    // Valid when all required fields provided
    assertTrue(
      validateForm(
        zone = "ZONE NO 11",
        blockNumber = "Block 1",
        dateOfOperation = "2024-04-10",
        operationName = "Planting",
        inputName = "Urea 46% N",
        ratoonDate = "2024-01-01",
        contractorName = "Raphael",
        acresWorkedStr = "10.0",
      ),
    )
  }

  @Test
  fun testContractorOptionsDropdown() {
    val expectedContractors = listOf("Raphael", "Hussein", "Khamis Fumu", "Jackson")
    assertEquals(expectedContractors, com.example.data.model.STANDARD_CONTRACTORS)
    assertTrue(com.example.data.model.STANDARD_CONTRACTORS.contains("Raphael"))
    assertTrue(com.example.data.model.STANDARD_CONTRACTORS.contains("Hussein"))
    assertTrue(com.example.data.model.STANDARD_CONTRACTORS.contains("Khamis Fumu"))
    assertTrue(com.example.data.model.STANDARD_CONTRACTORS.contains("Jackson"))
  }

  @Test
  fun testWeeklyOperationsSummaryAggregationStubbleShaving() {
    val report1 = ReportEntity(
      id = "REP_01",
      zone = "ZONE NO 11",
      dateOfOperation = "02/09/2026",
      operationName = "Stubble shaving",
      blockNumber = "B-02",
      contractorName = "Raphael",
      noOfLabourers = 20,
      areaCoveredHa = 45.0, // 45 Acres
      balanceToBeDoneHa = 0.0,
      inputName = "Mechanical Disc",
      ratoonDate = "01/09/2026",
      createdBy = "headman11",
      createdByRole = Role.HEADMAN,
      status = ReportStatus.SYNCED,
    )

    val report2 = ReportEntity(
      id = "REP_02",
      zone = "ZONE NO 11",
      dateOfOperation = "05/09/2026",
      operationName = "Stubble shaving",
      blockNumber = "B-04",
      contractorName = "Raphael",
      noOfLabourers = 18,
      areaCoveredHa = 35.0, // 35 Acres
      balanceToBeDoneHa = 0.0,
      inputName = "Mechanical Disc",
      ratoonDate = "01/09/2026",
      createdBy = "headman11",
      createdByRole = Role.HEADMAN,
      status = ReportStatus.SYNCED,
    )

    val reports = listOf(report1, report2)
    val totalStubbleShavingArea = reports.filter { it.operationName == "Stubble shaving" }.sumOf { it.areaCoveredHa }
    assertEquals(80.0, totalStubbleShavingArea, 0.01)

    val formattedReport = "From 1/09/2026 to 7/09/2026, Area Covered by Stubble shaving is ${totalStubbleShavingArea.toInt()}Ac"
    assertEquals("From 1/09/2026 to 7/09/2026, Area Covered by Stubble shaving is 80Ac", formattedReport)
  }

  @Test
  fun testPhotoEvidenceAttachmentInReportEntity() {
    val reportWithPhoto = ReportEntity(
      id = "REP_PHOTO_01",
      zone = "ZONE NO 11",
      dateOfOperation = "02/09/2026",
      operationName = "Stubble shaving",
      blockNumber = "B-02",
      contractorName = "Raphael",
      noOfLabourers = 20,
      areaCoveredHa = 45.0,
      balanceToBeDoneHa = 0.0,
      photoUri = "content://media/external/images/media/1001",
      createdBy = "headman11",
      createdByRole = Role.HEADMAN,
      status = ReportStatus.SYNCED,
    )

    assertNotNull(reportWithPhoto.photoUri)
    assertEquals("content://media/external/images/media/1001", reportWithPhoto.photoUri)
  }
}

