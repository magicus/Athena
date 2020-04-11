package com.wynntils.athena.routes

import com.google.gson.JsonObject
import com.wynntils.athena.core.asJson
import com.wynntils.athena.core.getOrCreate
import com.wynntils.athena.core.routes.annotations.BasePath
import com.wynntils.athena.core.routes.annotations.Route
import com.wynntils.athena.core.routes.enums.RouteType
import com.wynntils.athena.database.DatabaseManager
import io.javalin.http.Context
import org.json.simple.JSONArray
import org.json.simple.JSONObject

/**
 * All user related routes
 * Base Path: /user
 * Required Parameters: TOKEN
 *
 * Routes:
 *  POST /updateDiscord
 *  POST /uploadConfigs
 */
@BasePath("/user")
class UserRoutes {

    /**
     * Updates the user Discord Information
     * Required Body: authToken, id, username
     */
    @Route(path = "/updateDiscord", type = RouteType.POST)
    fun updateDiscord(ctx: Context): JSONObject {
        val response = JSONObject()
        val body = ctx.body().asJson<JsonObject>()

        if (!body.has("authToken") || !body.has("id") || !body.has("username")) {
            ctx.status(400)

            response["message"] = "Expecting parameters 'authToken', 'id' and 'username'."
            return response
        }

        val user = DatabaseManager.getUserProfile(body["authToken"].asString)
        if (user == null) {
            ctx.status(400)

            response["message"] = "The provided Authorization Token is invalid."
            return response
        }

        user.updateDiscord(body["id"].asString, body["username"].asString)

        ctx.status(200)
        response["message"] = "Successfully updated ${user.username} Discord Information."
        return response
    }

    /**
     * Handle e stores User Configurations
     * Required Body: MULTIPART FORM (authToken; config)
     */
    @Route(path = "/uploadConfigs", type = RouteType.POST)
    fun uploadConfigs(ctx: Context): JSONObject {
        val response = JSONObject()
        if (!ctx.isMultipartFormData() || ctx.formParams("authToken").isEmpty() || ctx.uploadedFiles("config").isEmpty()) {
            ctx.status(400)

            response["message"] = "Expecting MultiPart Form, containing 'authToken' and 'config'."
            return response
        }

        val user = DatabaseManager.getUserProfile(ctx.formParams("authToken").first())
        if (user == null) {
            ctx.status(400)

            response["message"] = "The provided Authorization Token is invalid."
            return response
        }

        val uploadResult = response.getOrCreate<JSONArray>("results")
        for (file in ctx.uploadedFiles("config")) {
            val fileResult = JSONObject()
            uploadResult.add(fileResult)
            fileResult["name"] = file.filename

            if (file.size > 200000) { // bigger than 200kbp
                fileResult["message"] = "The provided configuration is bigger than 200 kilobytes."
                continue
            }
            if (user.getConfigAmount() >= 80) {
                fileResult["message"] = "User exceeded the configuration amount limit."
                continue
            }

            user.setConfig(file.filename, file.content.readBytes())
            fileResult["message"] = "Configuration stored successfully."
        }

        return response
    }

}