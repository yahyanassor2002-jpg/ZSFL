package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CloudSyncService(private val context: Context) {

  private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .writeTimeout(20, TimeUnit.SECONDS)
    .build()

  companion object {
    private const val TAG = "CloudSyncService"
    // Dedicated production cloud backend for UPENJAnet shared across all user phones
    private const val CLOUD_OBJECT_ID = "ff808181a09d98f701a0b605409f38e9"
    private const val BASE_URL = "https://api.restful-api.dev/objects"
  }

  fun isNetworkAvailable(): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }

  data class CloudDataPayload(
    val reports: List<ReportEntity>,
    val discussions: List<DiscussionEntity>,
    val zoneProgress: List<ZoneProgressEntity>,
    val users: List<UserEntity>,
    val deletedReportIds: List<String> = emptyList(),
    val lastUpdated: Long,
  )

  suspend fun fetchCloudData(): Result<CloudDataPayload> = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) {
      return@withContext Result.failure(IllegalStateException("No internet connection available. Offline mode active."))
    }

    try {
      val request = Request.Builder()
        .url("$BASE_URL/$CLOUD_OBJECT_ID")
        .get()
        .build()

      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          return@withContext Result.failure(Exception("Cloud server returned error HTTP ${response.code}"))
        }

        val bodyStr = response.body?.string().orEmpty()
        if (bodyStr.isBlank()) {
          return@withContext Result.failure(Exception("Empty response from cloud server"))
        }

        val rootObj = JSONObject(bodyStr)
        val dataObj = rootObj.optJSONObject("data") ?: JSONObject()

        val parsedReports = parseReports(dataObj.optJSONArray("reports"))
        val parsedDiscussions = parseDiscussions(dataObj.optJSONArray("discussions"))
        val parsedProgress = parseZoneProgress(dataObj.optJSONArray("zoneProgress"))
        val parsedUsers = parseUsers(dataObj.optJSONArray("users"))
        val parsedDeleted = parseStringList(dataObj.optJSONArray("deletedReportIds"))
        val lastUpdated = dataObj.optLong("lastUpdated", System.currentTimeMillis())

        Result.success(
          CloudDataPayload(
            reports = parsedReports,
            discussions = parsedDiscussions,
            zoneProgress = parsedProgress,
            users = parsedUsers,
            deletedReportIds = parsedDeleted,
            lastUpdated = lastUpdated,
          )
        )
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error fetching from cloud: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun uploadCloudData(payload: CloudDataPayload): Result<Unit> = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) {
      return@withContext Result.failure(IllegalStateException("No internet connection available. Stored locally."))
    }

    try {
      val dataObj = JSONObject().apply {
        put("reports", serializeReports(payload.reports))
        put("discussions", serializeDiscussions(payload.discussions))
        put("zoneProgress", serializeZoneProgress(payload.zoneProgress))
        put("users", serializeUsers(payload.users))
        put("deletedReportIds", serializeStringList(payload.deletedReportIds))
        put("lastUpdated", System.currentTimeMillis())
      }

      val rootObj = JSONObject().apply {
        put("name", "UPENJAnet_Production_Shared_Database")
        put("data", dataObj)
      }

      val jsonMedia = "application/json; charset=utf-8".toMediaType()
      val body = rootObj.toString().toRequestBody(jsonMedia)

      val request = Request.Builder()
        .url("$BASE_URL/$CLOUD_OBJECT_ID")
        .put(body)
        .build()

      client.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          Result.success(Unit)
        } else {
          Result.failure(Exception("Cloud update failed with HTTP ${response.code}"))
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error uploading to cloud: ${e.message}", e)
      Result.failure(e)
    }
  }

  // Helper parsers and serializers
  private fun parseReports(arr: JSONArray?): List<ReportEntity> {
    if (arr == null) return emptyList()
    val list = mutableListOf<ReportEntity>()
    for (i in 0 until arr.length()) {
      val obj = arr.optJSONObject(i) ?: continue
      try {
        list.add(
          ReportEntity(
            id = obj.optString("id"),
            zone = obj.optString("zone", "ZONE NO 11"),
            dateOfOperation = obj.optString("dateOfOperation"),
            operationName = obj.optString("operationName"),
            blockNumber = obj.optString("blockNumber"),
            contractorName = obj.optString("contractorName"),
            noOfLabourers = obj.optInt("noOfLabourers", 0),
            areaCoveredHa = obj.optDouble("areaCoveredHa", 0.0),
            balanceToBeDoneHa = obj.optDouble("balanceToBeDoneHa", 0.0),
            inputName = obj.optString("inputName", ""),
            ratoonDate = obj.optString("ratoonDate", ""),
            remark = obj.optString("remark", ""),
            photoUri = obj.optString("photoUri", "").takeIf { it.isNotBlank() },
            createdBy = obj.optString("createdBy", "Admin"),
            createdByRole = try { Role.valueOf(obj.optString("createdByRole", "HEADMAN")) } catch (e: Exception) { Role.HEADMAN },
            headmanPhone = obj.optString("headmanPhone", ""),
            adminComment = obj.optString("adminComment", ""),
            adminCommentAuthor = obj.optString("adminCommentAuthor", ""),
            adminCommentDate = obj.optString("adminCommentDate", ""),
            isDeleted = obj.optBoolean("isDeleted", false),
            lastUpdatedBy = obj.optString("lastUpdatedBy", obj.optString("createdBy", "Admin")),
            status = ReportStatus.SYNCED,
            synced = true,
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
          )
        )
      } catch (e: Exception) {
        Log.w(TAG, "Failed parsing report item: ${e.message}")
      }
    }
    return list
  }

  private fun serializeReports(reports: List<ReportEntity>): JSONArray {
    val arr = JSONArray()
    for (r in reports) {
      val obj = JSONObject().apply {
        put("id", r.id)
        put("zone", r.zone)
        put("dateOfOperation", r.dateOfOperation)
        put("operationName", r.operationName)
        put("blockNumber", r.blockNumber)
        put("contractorName", r.contractorName)
        put("noOfLabourers", r.noOfLabourers)
        put("areaCoveredHa", r.areaCoveredHa)
        put("balanceToBeDoneHa", r.balanceToBeDoneHa)
        put("inputName", r.inputName)
        put("ratoonDate", r.ratoonDate)
        put("remark", r.remark)
        put("photoUri", r.photoUri ?: "")
        put("createdBy", r.createdBy)
        put("createdByRole", r.createdByRole.name)
        put("headmanPhone", r.headmanPhone)
        put("adminComment", r.adminComment)
        put("adminCommentAuthor", r.adminCommentAuthor)
        put("adminCommentDate", r.adminCommentDate)
        put("isDeleted", r.isDeleted)
        put("lastUpdatedBy", r.lastUpdatedBy)
        put("status", ReportStatus.SYNCED.name)
        put("synced", true)
        put("createdAt", r.createdAt)
        put("updatedAt", r.updatedAt)
      }
      arr.put(obj)
    }
    return arr
  }

  private fun parseDiscussions(arr: JSONArray?): List<DiscussionEntity> {
    if (arr == null) return emptyList()
    val list = mutableListOf<DiscussionEntity>()
    for (i in 0 until arr.length()) {
      val obj = arr.optJSONObject(i) ?: continue
      try {
        list.add(
          DiscussionEntity(
            id = obj.optString("id"),
            title = obj.optString("title"),
            notes = obj.optString("notes"),
            division = obj.optString("division"),
            date = obj.optString("date"),
            author = obj.optString("author"),
            authorRole = try { Role.valueOf(obj.optString("authorRole", "ADMIN")) } catch (e: Exception) { Role.ADMIN },
            synced = true,
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
            lastUpdatedBy = obj.optString("lastUpdatedBy", obj.optString("author")),
          )
        )
      } catch (e: Exception) {
        Log.w(TAG, "Failed parsing discussion item: ${e.message}")
      }
    }
    return list
  }

  private fun serializeDiscussions(discussions: List<DiscussionEntity>): JSONArray {
    val arr = JSONArray()
    for (d in discussions) {
      val obj = JSONObject().apply {
        put("id", d.id)
        put("title", d.title)
        put("notes", d.notes)
        put("division", d.division)
        put("date", d.date)
        put("author", d.author)
        put("authorRole", d.authorRole.name)
        put("synced", true)
        put("createdAt", d.createdAt)
        put("updatedAt", d.updatedAt)
        put("lastUpdatedBy", d.lastUpdatedBy)
      }
      arr.put(obj)
    }
    return arr
  }

  private fun parseZoneProgress(arr: JSONArray?): List<ZoneProgressEntity> {
    if (arr == null) return emptyList()
    val list = mutableListOf<ZoneProgressEntity>()
    for (i in 0 until arr.length()) {
      val obj = arr.optJSONObject(i) ?: continue
      try {
        list.add(
          ZoneProgressEntity(
            id = obj.optString("id"),
            zone = obj.optString("zone"),
            date = obj.optString("date"),
            activity = obj.optString("activity"),
            progressStatus = obj.optString("progressStatus", "In Progress"),
            percentage = obj.optInt("percentage", 0),
            remarks = obj.optString("remarks", ""),
            submittedBy = obj.optString("submittedBy"),
            synced = true,
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
            lastUpdatedBy = obj.optString("lastUpdatedBy", obj.optString("submittedBy")),
          )
        )
      } catch (e: Exception) {
        Log.w(TAG, "Failed parsing progress item: ${e.message}")
      }
    }
    return list
  }

  private fun serializeZoneProgress(progressList: List<ZoneProgressEntity>): JSONArray {
    val arr = JSONArray()
    for (p in progressList) {
      val obj = JSONObject().apply {
        put("id", p.id)
        put("zone", p.zone)
        put("date", p.date)
        put("activity", p.activity)
        put("progressStatus", p.progressStatus)
        put("percentage", p.percentage)
        put("remarks", p.remarks)
        put("submittedBy", p.submittedBy)
        put("synced", true)
        put("createdAt", p.createdAt)
        put("updatedAt", p.updatedAt)
        put("lastUpdatedBy", p.lastUpdatedBy)
      }
      arr.put(obj)
    }
    return arr
  }

  private fun parseStringList(arr: JSONArray?): List<String> {
    if (arr == null) return emptyList()
    val list = mutableListOf<String>()
    for (i in 0 until arr.length()) {
      val str = arr.optString(i)
      if (str.isNotBlank()) list.add(str)
    }
    return list
  }

  private fun serializeStringList(list: List<String>): JSONArray {
    val arr = JSONArray()
    for (s in list) {
      arr.put(s)
    }
    return arr
  }

  private fun parseUsers(arr: JSONArray?): List<UserEntity> {
    if (arr == null) return emptyList()
    val list = mutableListOf<UserEntity>()
    for (i in 0 until arr.length()) {
      val obj = arr.optJSONObject(i) ?: continue
      try {
        list.add(
          UserEntity(
            id = obj.optString("id"),
            username = obj.optString("username"),
            passwordHash = obj.optString("passwordHash"),
            role = try { Role.valueOf(obj.optString("role", "HEADMAN")) } catch (e: Exception) { Role.HEADMAN },
            zoneId = if (obj.has("zoneId") && !obj.isNull("zoneId")) obj.getString("zoneId") else null,
            phoneNumber = obj.optString("phoneNumber", ""),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
          )
        )
      } catch (e: Exception) {
        Log.w(TAG, "Failed parsing user item: ${e.message}")
      }
    }
    return list
  }

  private fun serializeUsers(users: List<UserEntity>): JSONArray {
    val arr = JSONArray()
    for (u in users) {
      val obj = JSONObject().apply {
        put("id", u.id)
        put("username", u.username)
        put("passwordHash", u.passwordHash)
        put("role", u.role.name)
        if (u.zoneId != null) put("zoneId", u.zoneId)
        put("phoneNumber", u.phoneNumber)
        put("createdAt", u.createdAt)
      }
      arr.put(obj)
    }
    return arr
  }
}
