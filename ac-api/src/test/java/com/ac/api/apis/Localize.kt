package com.ac.api.apis

import com.ac.api.infrastructure.ApiClient
import com.ac.api.models.ImageDescription
import com.ac.api.models.ImageDescriptionGps
import com.ac.api.models.LocalizationHint
import com.ac.api.models.LocalizationResult
import io.kotlintest.specs.StringSpec
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.Assertions
import retrofit2.Call
import retrofit2.Response
import java.io.File

const val EXP_2 = "LocalizationResult(status=LocalizationStatus(code=0, message=Image has been localized in recId: <data/DB/series/series_2020-08-05_12-30-24.364291954.140389646821120/1>), camera=Camera(pose=Pose(position=Vector3d(x=-0.93245953, y=0.34045383, z=-1.2374065), orientation=Quaternion(w=0.99095976, x=0.079309344, y=0.10731971, z=0.013828045)), intrinsics=null), reconstructionId=14396, placeholders=[PlaceholderNode3d(placeholderId=objects/object_5743, pose=Pose(position=Vector3d(x=2.5792007, y=-9.540814, z=-26.873825), orientation=Quaternion(w=1.0, x=0.0, y=0.0, z=0.0)), frame=[Vector3d(x=-1.5821398, y=9.340682, z=29.435549), Vector3d(x=-1.93151, y=9.3245945, z=26.864841), Vector3d(x=-1.708619, y=9.128212, z=27.603172), Vector3d(x=5.2222686, y=-27.793488, z=-83.903564)]), PlaceholderNode3d(placeholderId=objects/object_5744, pose=Pose(position=Vector3d(x=-0.66011304, y=0.07447926, z=4.1616354), orientation=Quaternion(w=1.0, x=0.0, y=0.0, z=0.0)), frame=[Vector3d(x=-0.6374325, y=-0.49582952, z=-0.4958724), Vector3d(x=0.5251812, y=-0.73076963, z=0.41055706), Vector3d(x=0.657375, y=0.5989117, z=0.5112318), Vector3d(x=-0.5451237, y=0.62768745, z=-0.42591643)]), PlaceholderNode3d(placeholderId=objects/object_5767, pose=Pose(position=Vector3d(x=0.2973865, y=0.22012317, z=4.6855145), orientation=Quaternion(w=1.0, x=0.0, y=0.0, z=0.0)), frame=[Vector3d(x=-0.6790661, y=-0.51389605, z=-0.78493804), Vector3d(x=0.62414813, y=-0.7032086, z=0.29976252), Vector3d(x=0.76932794, y=0.67632174, z=0.9230349), Vector3d(x=-0.7144099, y=0.5407829, z=-0.43785933)]), PlaceholderNode3d(placeholderId=objects/object_5768, pose=Pose(position=Vector3d(x=-1.0481468, y=-0.022465922, z=3.6530092), orientation=Quaternion(w=1.0, x=0.0, y=0.0, z=0.0)), frame=[Vector3d(x=-0.6564383, y=-0.46352622, z=-0.6437617), Vector3d(x=0.58267486, y=-0.7263348, z=0.40206867), Vector3d(x=0.6915339, y=0.57455736, z=0.6910175), Vector3d(x=-0.61777055, y=0.6153037, z=-0.4493245)]), PlaceholderNode3d(placeholderId=objects/object_5769, pose=Pose(position=Vector3d(x=-0.9040135, y=0.050115466, z=3.7182782), orientation=Quaternion(w=1.0, x=0.0, y=0.0, z=0.0)), frame=[Vector3d(x=-0.7755006, y=-1.644074, z=-1.091977), Vector3d(x=0.7800353, y=-2.292092, z=0.1917501), Vector3d(x=0.8855231, y=2.241948, z=1.3306777), Vector3d(x=-0.8900578, y=1.6942182, z=-0.4304509)]), PlaceholderNode3d(placeholderId=objects/object_5899, pose=Pose(position=Vector3d(x=0.6471708, y=0.09417622, z=2.0049615), orientation=Quaternion(w=1.0, x=0.0, y=0.0, z=0.0)), frame=[Vector3d(x=0.24539019, y=-0.20206156, z=-0.5859046), Vector3d(x=0.31848976, y=-0.21899393, z=-0.42593065), Vector3d(x=-0.35139495, y=0.32596102, z=1.1221104), Vector3d(x=-0.212485, y=0.095094465, z=-0.11027508)]), PlaceholderNode3d(placeholderId=objects/object_5905, pose=Pose(position=Vector3d(x=-1.0792906, y=-0.06819612, z=3.0692863), orientation=Quaternion(w=1.0, x=0.0, y=0.0, z=0.0)), frame=[Vector3d(x=-0.28866073, y=-0.28476003, z=-0.46777725), Vector3d(x=0.3763408, y=-0.4693776, z=0.109924845), Vector3d(x=0.3343014, y=0.28303143, z=0.51393384), Vector3d(x=-0.42198148, y=0.4711062, z=-0.15608144)]), PlaceholderNode3d(placeholderId=objects/object_5906, pose=Pose(position=Vector3d(x=-1.1483434, y=0.091093615, z=3.6206777), orientation=Quaternion(w=1.0, x=0.0, y=0.0, z=0.0)), frame=[Vector3d(x=-0.45740876, y=-0.40870708, z=-0.4200439), Vector3d(x=0.44984037, y=-0.55694413, z=0.23640919), Vector3d(x=0.49041393, y=0.38204262, z=0.44000572), Vector3d(x=-0.48284554, y=0.5836086, z=-0.25637102)]), PlaceholderNode3d(placeholderId=objects/object_5907, pose=Pose(position=Vector3d(x=-0.9331494, y=-0.3159004, z=3.5402), orientation=Quaternion(w=1.0, x=0.0, y=0.0, z=0.0)), frame=[Vector3d(x=-0.39697528, y=-0.40260062, z=-0.43435496), Vector3d(x=0.46134415, y=-0.5146374, z=0.2129358), Vector3d(x=0.43432477, y=0.37424108, z=0.45554578), Vector3d(x=-0.49869362, y=0.542997, z=-0.23412663)]), PlaceholderNode3d(placeholderId=objects/object_5908, pose=Pose(position=Vector3d(x=-0.9405509, y=0.01740847, z=4.0119915), orientation=Quaternion(w=1.0, x=0.0, y=0.0, z=0.0)), frame=[Vector3d(x=-0.5500747, y=-0.40990338, z=-0.34480208), Vector3d(x=0.4319645, y=-0.5525892, z=0.29679257), Vector3d(x=0.55225885, y=0.3973531, z=0.34659314), Vector3d(x=-0.43414867, y=0.5651395, z=-0.29858363)]), PlaceholderNode3d(placeholderId=objects/object_6474, pose=Pose(position=Vector3d(x=1.0680554, y=-0.58173007, z=4.246565), orientation=Quaternion(w=1.0, x=0.0, y=0.0, z=0.0)), frame=[Vector3d(x=-1.084719, y=-0.61292887, z=1.9094715), Vector3d(x=0.6646357, y=-0.49494275, z=1.0209609), Vector3d(x=0.49958813, y=0.530364, z=-1.5038005), Vector3d(x=-0.07950493, y=0.5775076, z=-1.4266318)])], surfaces=null, objects=[ARObject(placeholder=Placeholder(placeholderId=objects/object_5743), sticker={created_by=vladimir.kocherizhkin@vergendo.com, creation_date=1596621840923, description=, path=mail.ru, sticker_id=objects/object_5743, sticker_subtype=, sticker_text=1112, sticker_type=restaurant}), ARObject(placeholder=Placeholder(placeholderId=objects/object_5744), sticker={created_by=vladimir.kocherizhkin@vergendo.com, creation_date=1596622252776, description=, path=site.ru, sticker_id=objects/object_5744, sticker_subtype=, sticker_text=стена, sticker_type=restaurant}), ARObject(placeholder=Placeholder(placeholderId=objects/object_5767), sticker={created_by=vladimir.kocherizhkin@vergendo.com, creation_date=1597319447954, description=, path=маилру, sticker_id=objects/object_5767, sticker_subtype=, sticker_text=111, sticker_type=restaurant}), ARObject(placeholder=Placeholder(placeholderId=objects/object_5768), sticker={created_by=vladimir.kocherizhkin@vergendo.com, creation_date=1597321229794, description=, path=mail.ru, sticker_id=objects/object_5768, sticker_subtype=, sticker_text=new, sticker_type=restaurant}), ARObject(placeholder=Placeholder(placeholderId=objects/object_5769), sticker={created_by=vladimir.kocherizhkin@vergendo.com, creation_date=1597321317027, description=, path=hhh, sticker_id=objects/object_5769, sticker_subtype=, sticker_text=ggg, sticker_type=restaurant}), ARObject(placeholder=Placeholder(placeholderId=objects/object_5899), sticker={created_by=vladimir.kocherizhkin@vergendo.com, creation_date=1599216518172, description=, path=ррр, sticker_id=objects/object_5899, sticker_subtype=, sticker_text=1112, sticker_type=other}), ARObject(placeholder=Placeholder(placeholderId=objects/object_5905), sticker={created_by=vladimir.kocherizhkin@vergendo.com, creation_date=1599220836370, description=, path=роо, sticker_id=objects/object_5905, sticker_subtype=, sticker_text=еен, sticker_type=other}), ARObject(placeholder=Placeholder(placeholderId=objects/object_5906), sticker={created_by=vladimir.kocherizhkin@vergendo.com, creation_date=1599221039967, description=, path=пррр, sticker_id=objects/object_5906, sticker_subtype=, sticker_text=ннн, sticker_type=other}), ARObject(placeholder=Placeholder(placeholderId=objects/object_5907), sticker={created_by=vladimir.kocherizhkin@vergendo.com, creation_date=1599221138786, description=, path=ооо, sticker_id=objects/object_5907, sticker_subtype=, sticker_text=ннн, sticker_type=other}), ARObject(placeholder=Placeholder(placeholderId=objects/object_5908), sticker={created_by=vladimir.kocherizhkin@vergendo.com, creation_date=1599221614120, description=, path=иррр, sticker_id=objects/object_5908, sticker_subtype=, sticker_text=тьт, sticker_type=other}), ARObject(placeholder=Placeholder(placeholderId=objects/object_6474), sticker={created_by=vladimir.kocherizhkin@vergendo.com, creation_date=1601295413551, description=, path=, sticker_id=objects/object_6474, sticker_subtype=, sticker_text=ооо, sticker_type=other})])"
class Localize : StringSpec({
    "Localize" {
        val apiClient = ApiClient("http://developer.augmented.city/api/v2")
        val webService = apiClient.createService(LocalizerApi::class.java)
        val gps = ImageDescriptionGps(59.9927142, 30.3807979, 67.9000015258789)
        val imageDesc = ImageDescription(gps, null, null,false, 90)
        val image = Localize::class.java.getResource("image_1601547820001_lat_59.9927142_lon_30.3807979_alt_67.9000015258789.jpg")!!.readBytes()
        val mp = createMultipartBody(image)
        val hint = LocalizationHint( emptyList(), null)
        val result: Call<LocalizationResult> = webService.localize(imageDesc, mp, hint)
        try {
            val response: Response<LocalizationResult> = result.execute()
            println("LocalizationResult:" + response.body())
            Assertions.assertEquals(
                EXP_2, response.body()
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            assert(false)
        }
    }
})


