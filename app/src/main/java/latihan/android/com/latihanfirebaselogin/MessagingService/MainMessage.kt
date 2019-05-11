package latihan.android.com.latihanfirebaselogin.MessagingService

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import de.hdodenhof.circleimageview.CircleImageView
import latihan.android.com.latihanfirebaselogin.R

class MainMessage: AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    class MessageViewHolder(v: View): RecyclerView.ViewHolder(v){
        internal var messageTextView: TextView
        internal var messageImageView: ImageView
        internal var messengerTextView: TextView
        internal var messengerImageView: CircleImageView
        init {
            messageTextView = itemView.findViewById<View>(R.id.messsageTextView) as TextView
            messageImageView = itemView.findViewById<View>(R.id.messageImageView) as ImageView
            messengerTextView = itemView.findViewById<View>(R.id.messengerTextView) as TextView
            messengerImageView = itemView.findViewById<View>(R.id.messengerImageView) as CircleImageView
        }
    }
    private val TAG = "MainActivity"
    val MESSAGES_CHILD = "messages"
    private val REQUEST_INVITE = 1
    private val REQUEST_IMAGE = 2
    private val LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif"
    val ANONYMOUS= "anonymous"
    private var mUsername: String? = null
    private var mPhotoUrl: String? = null
    private var mSharedPreferences: SharedPreferences? = null
    private var mGoogleApiClient: GoogleApiClient? = null
    private var mSendButton: Button? = null
    private var mMessageRecyclerView: RecyclerView? = null
    private var mLinearLayoutManager: LinearLayoutManager? = null
    private var mProgressBar: ProgressBar? = null
    private var mMessageEditText: EditText? = null
    private var mAddMessageImageView: ImageView? = null
    //Firebase Instance variabels
    private var mFirebaseAuth: FirebaseAuth? = null
    private var mFirebaseUser: FirebaseUser? = null
    private var mFirebaseDatabaseReference: DatabaseReference? = null
    private var mFirebaseAdapter: FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_message)
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        //set Default username is Anonymous
        mUsername = ANONYMOUS

        mGoogleApiClient = GoogleApiClient.Builder(this).enableAutoManage(this, this).addApi(Auth.GOOGLE_SIGN_IN_API).build()
        //initialize progressbar and recyclerView
        mProgressBar = findViewById<View>(R.id.progressBar) as ProgressBar
        mMessageRecyclerView = findViewById<View>(R.id.messageRecyclerView) as RecyclerView
        mLinearLayoutManager = LinearLayoutManager(this)
        mLinearLayoutManager!!.stackFromEnd = true
        mMessageRecyclerView!!.layoutManager = mLinearLayoutManager
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().reference
        val parser = SnapshotParser<FriendlyMessage>{
            dataSnapshot ->
            val friendlyMessage = dataSnapshot.getValue(FriendlyMessage::class.java)
            if (friendlyMessage != null ){
                friendlyMessage!!.setId(dataSnapshot.key!!)
            }
            friendlyMessage!!
        }
        val  messagesRef = mFirebaseDatabaseReference!!.child(MESSAGES_CHILD)
        val options = FirebaseRecyclerOptions.Builder<FriendlyMessage>().setQuery(messagesRef, parser).build()
        mFirebaseAdapter = object : FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder>(options){
            override fun onCreateViewHolder(p0: ViewGroup, p1: Int): MessageViewHolder {
                val inflater = LayoutInflater.from(p0.context)
                return MessageViewHolder(inflater.inflate(R.layout.item_message, p0, false))
            }

            override fun onBindViewHolder(holder: MessageViewHolder, position: Int, model: FriendlyMessage) {
                mProgressBar!!.visibility = ProgressBar.INVISIBLE
                if (model.getText() != null){
                    holder.messageTextView.text = model.getText()
                    holder.messageTextView.visibility = TextView.VISIBLE
                    holder.messageImageView.visibility = ImageView.GONE
                }
                else if(model.getImageUrl() != null){
                    val imageUrl = model.getImageUrl()
                    if (imageUrl!!.startsWith("gs://")){
                        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
                        storageReference.downloadUrl.addOnCompleteListener { task ->
                            if (task.isSuccessful){
                                val downloadUrl = task.result!!.toString()
                                Glide.with(holder.messageImageView.context).load(downloadUrl).into(holder.messageImageView)
                            }else{
                                Log.w(TAG, "GETTING DOWNLOAD"+"url was not succesfull.", task.exception)
                            }
                        }
                    }else{
                        Glide.with(holder.messageImageView.context).load(model.getImageUrl()!!).into(holder.messageImageView)
                    }
                    holder.messageImageView.visibility = ImageView.VISIBLE
                    holder.messageTextView.visibility = TextView.GONE
                }
                holder.messengerTextView.text = model.getName()
                if(model.getPhotoUrl()== null){
                    holder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(this@MainMessage, android.R.drawable.btn_star_big_off))
                }else{
                    Glide.with(this@MainMessage).load(model.getPhotoUrl()).into(holder.messengerImageView)
                }
            }
        }
        mFirebaseAdapter!!.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver(){
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val friendlyMessageCount = mFirebaseAdapter!!.itemCount
                val lastVisiblePosition = mLinearLayoutManager!!.findLastCompletelyVisibleItemPosition()
                if ((lastVisiblePosition == -1 || ((positionStart >= (friendlyMessageCount -1)&& lastVisiblePosition == (positionStart -1))))){
                    mMessageRecyclerView!!.scrollToPosition(positionStart)
                }
            }
        })
        mMessageRecyclerView!!.adapter = mFirebaseAdapter
        mMessageEditText = findViewById<View>(R.id.messageEditText) as EditText
        mMessageEditText!!.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
               if(s.toString().trim(){it<= ' '}.length>0){
                   mSendButton!!.isEnabled =true
               }else{
                   mSendButton!!.isEnabled = false
               }
            }

        })
        mSendButton = findViewById<View>(R.id.sendButton) as Button
        mSendButton!!.setOnClickListener( object: View.OnClickListener{
            override fun onClick(v: View?) {
                val friendlyMessage = FriendlyMessage(mMessageEditText!!.text.toString(), mUsername!!, mPhotoUrl!!, null)
                mFirebaseDatabaseReference!!.child(MESSAGES_CHILD).push().setValue(friendlyMessage)
                mMessageEditText!!.setText("")
            }
        })
        mAddMessageImageView = findViewById<View>(R.id.addMessageImageView) as ImageView
        mAddMessageImageView!!.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "image/*"
                startActivityForResult(intent, REQUEST_IMAGE)
            }
        })
        mFirebaseAuth = FirebaseAuth.getInstance()
        mFirebaseUser = mFirebaseAuth!!.currentUser
        if (mFirebaseUser == null){
            startActivity(Intent(this, ActivitySign::class.java))
            finish()
            return
        }else{
            mUsername = mFirebaseUser!!.displayName
            if (mFirebaseUser!!.photoUrl != null){
                mPhotoUrl = mFirebaseUser!!.photoUrl!!.toString()
            }
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onPause() {
        mFirebaseAdapter!!.stopListening()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mFirebaseAdapter!!.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.sign_out_menu->{
                mFirebaseAuth!!.signOut()
                Auth.GoogleSignInApi.signOut(mGoogleApiClient)
                mUsername = ANONYMOUS
                startActivity(Intent(this, ActivitySign::class.java))
                finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivity Result"+ "requestcode=$requestCode"+ "result code = $resultCode")
        if (requestCode == REQUEST_IMAGE){
            if (resultCode == Activity.RESULT_OK){
                if(data != null){
                    val uri = data.data
                    Log.d(TAG, "uri"+ uri!!.toString())
                    val tempMessage = FriendlyMessage(null, mUsername!!, mPhotoUrl!!, LOADING_IMAGE_URL)

                    mFirebaseDatabaseReference!!.child(MESSAGES_CHILD).push().setValue(tempMessage, object : DatabaseReference.CompletionListener{
                        override fun onComplete(p0: DatabaseError?, p1: DatabaseReference) {
                            if (p0 == null){
                                val key = p1.key
                                val storageReference = FirebaseStorage.getInstance().getReference(mFirebaseUser !!.uid).child(key!!).child(uri.lastPathSegment!!)
                                putImageInStorage(storageReference, uri, key)
                            }else{
                                Log.w(TAG, "unable to write nessage"+"message to database", p0!!.toException())
                            }
                        }

                    })
                }
            }
        }
    }

//    private fun putImageInStorage(storageReference: StorageReference, uri: Uri?, key: String) {
//        storageReference.putFile(uri!!).addOnCompleteListener(this@MainMessage, object: OnCompleteListener<UploadTask.TaskSnapshot>{
//            override fun onComplete(p0: Task<UploadTask.TaskSnapshot>) {
//                if (p0.isSuccessful){
//                    val friendlyMessage = FriendlyMessage(null,mUsername!!, mPhotoUrl!!, p0.result!!.toString())
//                    mFirebaseDatabaseReference!!.child(MESSAGES_CHILD).child(key!!).setValue(friendlyMessage)
//                }
//            }
//
//        })
//
//    }
private fun putImageInStorage(storageReference:
                              StorageReference, uri: Uri?, key:String?) {
    storageReference.putFile(uri!!)
        .addOnCompleteListener(this@MainMessage,
            object: OnCompleteListener<UploadTask.TaskSnapshot> {
                override fun onComplete(task: Task<UploadTask
                .TaskSnapshot>) {
                    if (task.isSuccessful) {
                        task.result!!.metadata!!
                            .reference!!.downloadUrl
                            .addOnCompleteListener(this@MainMessage,
                                object: OnCompleteListener<Uri> {
                                    override fun onComplete(task: Task<Uri>) {
                                        if (task.isSuccessful) {
                                            val friendlyMessage = FriendlyMessage(
                                                null, mUsername!!, mPhotoUrl!!,
                                                task.result!!.toString())
                                            mFirebaseDatabaseReference!!
                                                .child(MESSAGES_CHILD).child(key!!)
                                                .setValue(friendlyMessage)
                                        }
                                    }
                                })
                    }else{
                        Log.w(TAG, "Image upload" +
                                " task was not successful.",
                            task.exception)
                    }
                }
            })
}


}
//class MainMessage : AppCompatActivity(),
//    GoogleApiClient.OnConnectionFailedListener {
//
//    class MessageViewHolder(v: View):
//        RecyclerView.ViewHolder(v) {
//        internal var messageTextView: TextView
//        internal var messageImageView: ImageView
//        internal var messengerTextView: TextView
//        internal var messengerImageView: CircleImageView
//        init{
//            messageTextView = itemView.findViewById<View>(
//                R.id.messsageTextView) as TextView
//            messageImageView = itemView.findViewById<View>(
//                R.id.messageImageView) as ImageView
//            messengerTextView = itemView.findViewById<View>(
//                R.id.messengerTextView) as TextView
//            messengerImageView = itemView.findViewById<View>(
//                R.id.messengerImageView) as CircleImageView
//        }
//    }
//
//    private val TAG = "MainActivity"
//    val MESSAGES_CHILD = "messages"
//    private val REQUEST_INVITE = 1
//    private val REQUEST_IMAGE = 2
//    private val LOADING_IMAGE_URL =
//        "https://www.google.com/images/spin-32.gif"
//    val ANONYMOUS = "anonymous"
//    private var mUsername:String? = null
//    private var mPhotoUrl:String? = null
//    private var mSharedPreferences:
//            SharedPreferences? = null
//    private var mGoogleApiClient:GoogleApiClient? = null
//
//    private var mSendButton: Button? = null
//    private var mMessageRecyclerView: RecyclerView? = null
//    private var mLinearLayoutManager: LinearLayoutManager? = null
//    private var mProgressBar: ProgressBar? = null
//    private var mMessageEditText: EditText? = null
//    private var mAddMessageImageView: ImageView? = null
//    // Firebase instance variables
//    private var mFirebaseAuth: FirebaseAuth? = null
//    private var mFirebaseUser: FirebaseUser? = null
//    private var mFirebaseDatabaseReference: DatabaseReference?
//            = null
//    private var mFirebaseAdapter:
//            FirebaseRecyclerAdapter<FriendlyMessage,
//                    MessageViewHolder>? = null
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.main_message)
//        mSharedPreferences = PreferenceManager
//            .getDefaultSharedPreferences(this)
//        // Set default username is anonymous.
//        mUsername = ANONYMOUS
//
//        mGoogleApiClient = GoogleApiClient.Builder(this)
//            .enableAutoManage(this /* FragmentActivity */,
//                this /* OnConnectionFailedListener */)
//            .addApi(Auth.GOOGLE_SIGN_IN_API)
//            .build()
//
//        // Initialize ProgressBar and RecyclerView.
//        mProgressBar = findViewById<View>(R.id.progressBar)
//                as ProgressBar
//        mMessageRecyclerView = findViewById<View>(
//            R.id.messageRecyclerView) as RecyclerView
//        mLinearLayoutManager = LinearLayoutManager(this)
//        mLinearLayoutManager!!.stackFromEnd = true
//        mMessageRecyclerView!!.layoutManager =
//            mLinearLayoutManager
//        mFirebaseDatabaseReference =
//            FirebaseDatabase.getInstance().reference
//        val parser =
//            SnapshotParser<FriendlyMessage> {
//                    dataSnapshot ->
//                val friendlyMessage = dataSnapshot
//                    .getValue(FriendlyMessage::class.java)
//                if (friendlyMessage !=
//                    null /* no image */) {
//                    friendlyMessage!!.setId(dataSnapshot.key!!)
//                }
//                friendlyMessage!!
//            }
//
//        val messagesRef = mFirebaseDatabaseReference!!
//            .child(MESSAGES_CHILD)
//        val options = FirebaseRecyclerOptions
//            .Builder<FriendlyMessage>()
//            .setQuery(messagesRef, parser)
//            .build()
//        mFirebaseAdapter = object:
//            FirebaseRecyclerAdapter<FriendlyMessage,
//                    MessageViewHolder>(options) {
//            override fun onCreateViewHolder(viewGroup: ViewGroup,
//                                            i:Int):MessageViewHolder {
//                val inflater = LayoutInflater.from(viewGroup.context)
//                return MessageViewHolder(inflater.inflate(
//                    R.layout.item_message, viewGroup, false))
//            }
//
//            override fun onBindViewHolder(viewHolder:
//                                          MessageViewHolder,
//                                          position:Int, friendlyMessage:FriendlyMessage) {
//                mProgressBar!!.visibility = ProgressBar.INVISIBLE
//                if (friendlyMessage.getText() != null)
//                {
//                    viewHolder.messageTextView.text = friendlyMessage
//                        .getText()
//                    viewHolder.messageTextView.visibility =
//                        TextView.VISIBLE
//                    viewHolder.messageImageView.visibility =
//                        ImageView.GONE
//                }
//                else if (friendlyMessage.getImageUrl() != null){
//                    val imageUrl = friendlyMessage.getImageUrl()
//                    if (imageUrl!!.startsWith("gs://")) {
//                        val storageReference = FirebaseStorage
//                            .getInstance()
//                            .getReferenceFromUrl(imageUrl)
//                        storageReference.downloadUrl
//                            .addOnCompleteListener { task ->
//                                if (task.isSuccessful) {
//                                    val downloadUrl = task.result!!
//                                        .toString()
//                                    Glide.with(viewHolder
//                                        .messageImageView.context)
//                                        .load(downloadUrl)
//                                        .into(viewHolder.messageImageView)
//                                } else {
//                                    Log.w(TAG, "Getting download " +
//                                            "url was not successful.",
//                                        task.exception
//                                    )
//                                }
//                            }
//                    }else {
//                        Glide.with(viewHolder.messageImageView.context)
//                            .load(friendlyMessage.getImageUrl()!!)
//                            .into(viewHolder.messageImageView)
//                    }
//                    viewHolder.messageImageView.visibility = ImageView.VISIBLE
//                    viewHolder.messageTextView.visibility = TextView.GONE
//                }
//                viewHolder.messengerTextView.text = friendlyMessage
//                    .getName()
//                if (friendlyMessage.getPhotoUrl() == null) {
//                    viewHolder.messengerImageView.setImageDrawable(
//                        ContextCompat.getDrawable(
//                            this@MainMessage,
//                            android.R.drawable.btn_star_big_off))
//                }else{
//                    Glide.with(this@MainMessage)
//                        .load(friendlyMessage.getPhotoUrl())
//                        .into(viewHolder.messengerImageView)
//                }
//            }
//        }
//        mFirebaseAdapter!!.registerAdapterDataObserver(
//            object: RecyclerView.AdapterDataObserver() {
//                override fun onItemRangeInserted(positionStart:Int,
//                                                 itemCount:Int) {
//                    super.onItemRangeInserted(positionStart, itemCount)
//                    val friendlyMessageCount = mFirebaseAdapter!!.itemCount
//                    val lastVisiblePosition = mLinearLayoutManager!!
//                        .findLastCompletelyVisibleItemPosition()
//                    if ((lastVisiblePosition == -1 || (
//                                (positionStart >=
//                                        (friendlyMessageCount - 1)
//                                        && lastVisiblePosition ==
//                                        (positionStart - 1))))) {
//                        mMessageRecyclerView!!.scrollToPosition(
//                            positionStart)
//                    }
//                }
//            })
//        mMessageRecyclerView!!.adapter = mFirebaseAdapter
//
//        mMessageEditText = findViewById<View>(R.id
//            .messageEditText) as EditText
//        mMessageEditText!!.addTextChangedListener(
//            object: TextWatcher {
//                override fun beforeTextChanged(
//                    charSequence:CharSequence, i:Int,
//                    i1:Int, i2:Int) {}
//                override fun onTextChanged(charSequence
//                                           :CharSequence, i:Int, i1:Int, i2:Int) {
//                    if (charSequence.toString().trim { it <= ' ' }
//                            .length > 0) {
//                        mSendButton!!.isEnabled = true
//                    }else {
//                        mSendButton!!.isEnabled = false
//                    }
//                }
//                override fun afterTextChanged(editable: Editable) {}
//            })
//        mSendButton = findViewById<View>(
//            R.id.sendButton) as Button
//        mSendButton!!.setOnClickListener(
//            object: View.OnClickListener {
//                override fun onClick(view: View) {
//                    val friendlyMessage = FriendlyMessage(
//                        mMessageEditText!!.text.toString(),
//                        mUsername!!,
//                        mPhotoUrl!!, null)
//                    mFirebaseDatabaseReference!!
//                        .child(MESSAGES_CHILD)
//                        .push().setValue(friendlyMessage)
//                    mMessageEditText!!.setText("")
//                }
//            })
//        mAddMessageImageView = findViewById<View>(
//            R.id.addMessageImageView) as ImageView
//        mAddMessageImageView!!.setOnClickListener(object:
//            View.OnClickListener {
//            override fun onClick(view: View) {
//                // Select image for image message on click.
//                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
//                intent.addCategory(Intent.CATEGORY_OPENABLE)
//                intent.type = "image/*"
//                startActivityForResult(intent, REQUEST_IMAGE)
//            }
//        })
//
//        mFirebaseAuth = FirebaseAuth.getInstance()
//        mFirebaseUser = mFirebaseAuth!!.currentUser
//        if (mFirebaseUser == null){
//            // Not signed in, launch the Sign In activity
//            startActivity(Intent(this,
//                ActivitySign::class.java))
//            finish()
//            return
//        }else{
//            mUsername = mFirebaseUser!!.displayName
//            if (mFirebaseUser!!.photoUrl != null) {
//                mPhotoUrl = mFirebaseUser!!
//                    .photoUrl!!.toString()
//            }
//        }
//    }
//
//    public override fun onStart() {
//        super.onStart() }
//    public override fun onPause() {
//        mFirebaseAdapter!!.stopListening()
//        super.onPause() }
//    public override fun onResume() {
//        super.onResume()
//        mFirebaseAdapter!!.startListening() }
//    public override fun onDestroy() {
//        super.onDestroy() }
//    override fun onCreateOptionsMenu(menu: Menu)
//            :Boolean {
//        val inflater = menuInflater
//        inflater.inflate(R.menu.main_menu, menu)
//        return true }
//
//    override fun onOptionsItemSelected(item: MenuItem)
//            :Boolean {
//        when (item.itemId) {
//            R.id.sign_out_menu -> {
//                mFirebaseAuth!!.signOut()
//                Auth.GoogleSignInApi.signOut(mGoogleApiClient)
//                mUsername = ANONYMOUS
//                startActivity(Intent(this,
//                    ActivitySign::class.java))
//                finish()
//                return true
//            }
//            else -> return super
//                .onOptionsItemSelected(item)
//        }
//    }
//
//    override fun onConnectionFailed(connectionResult:ConnectionResult) {
//        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
//        // be available.
//        Log.d(TAG, "onConnectionFailed:$connectionResult")
//        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show()
//    }
//
//    override fun onActivityResult(requestCode:Int,
//                                  resultCode:Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode,
//            data)
//        Log.d(TAG, "onActivityResult:" +
//                " requestCode=$requestCode," +
//                " resultCode=$resultCode")
//        if (requestCode == REQUEST_IMAGE) {
//            if (resultCode == Activity.RESULT_OK){
//                if (data != null) {
//                    val uri = data.data
//                    Log.d(TAG, "Uri: "
//                            + uri!!.toString())
//
//                    val tempMessage = FriendlyMessage(
//                        null, mUsername!!, mPhotoUrl!!,
//                        LOADING_IMAGE_URL)
//                    mFirebaseDatabaseReference!!
//                        .child(MESSAGES_CHILD).push()
//                        .setValue(tempMessage, object: DatabaseReference
//                        .CompletionListener {
//                            override fun onComplete(databaseError:
//                                                    DatabaseError?, databaseReference: DatabaseReference){
//                                if (databaseError == null) {
//                                    val key = databaseReference.key
//                                    val storageReference = FirebaseStorage
//                                        .getInstance()
//                                        .getReference(mFirebaseUser!!.uid)
//                                        .child(key!!)
//                                        .child(uri.lastPathSegment!!)
//                                    putImageInStorage(storageReference,
//                                        uri, key)
//                                }else{
//                                    Log.w(TAG, "Unable to write" +
//                                            " message to database.",
//                                        databaseError!!.toException())
//                                }
//                            }
//                        })
//                }
//            }
//        }
//    }
//
//    private fun putImageInStorage(storageReference:
//                                  StorageReference, uri: Uri?, key:String?) {
//        storageReference.putFile(uri!!)
//            .addOnCompleteListener(this@MainMessage,
//                object: OnCompleteListener<UploadTask.TaskSnapshot> {
//                    override fun onComplete(task: Task<UploadTask
//                    .TaskSnapshot>) {
//                        if (task.isSuccessful) {
//                            task.result!!.metadata!!
//                                .reference!!.downloadUrl
//                                .addOnCompleteListener(this@MainMessage,
//                                    object: OnCompleteListener<Uri> {
//                                        override fun onComplete(task: Task<Uri>) {
//                                            if (task.isSuccessful) {
//                                                val friendlyMessage = FriendlyMessage(
//                                                    null, mUsername!!, mPhotoUrl!!,
//                                                    task.result!!.toString())
//                                                mFirebaseDatabaseReference!!
//                                                    .child(MESSAGES_CHILD).child(key!!)
//                                                    .setValue(friendlyMessage)
//                                            }
//                                        }
//                                    })
//                        }else{
//                            Log.w(TAG, "Image upload" +
//                                    " task was not successful.",
//                                task.exception)
//                        }
//                    }
//                })
//    }
//
//}