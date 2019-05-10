package latihan.android.com.latihanfirebaselogin.MessagingService



class FriendlyMessage {
    private var id: String? = null
    private var text: String? = null
    private var name: String? = null
    private var photoUrl: String? = null
    private var imageUrl: String? = null
    constructor(){

    }
    constructor(text:String?, name: String, photoUrl : String, imageUrl: String? ){
        this.text = text
        this.name = name
        this.photoUrl = photoUrl
        this.imageUrl = imageUrl
    }
    fun getId(): String?{return id}
    fun setId(id: String){this.id = id}
    fun setText(text : String){this.text = text}
    fun getText(): String? {return text}
    fun getName(): String? {return name}
    fun setName(name: String){this.name = name}
    fun getPhotoUrl(): String? {return photoUrl}
    fun setPhotoUrl(photoUrl: String){this.photoUrl = photoUrl}
    fun getImageUrl(): String?{return imageUrl}
    fun setImageUrl(imageUrl: String){this.imageUrl = imageUrl}
}