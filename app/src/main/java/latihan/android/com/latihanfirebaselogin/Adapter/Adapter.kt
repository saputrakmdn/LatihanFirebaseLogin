package latihan.android.com.latihanfirebaselogin.Adapter

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.database.FirebaseDatabase
import latihan.android.com.latihanfirebaselogin.R
import latihan.android.com.latihanfirebaselogin.ShowData
import latihan.android.com.latihanfirebaselogin.add_data.Users

class Adapter(val mCtx: Context, val layoutResId: Int, val list : List<Users>): ArrayAdapter<Users>(mCtx, layoutResId, list) {
    override fun  getView(position: Int, convertView: View?, parent: ViewGroup): View{
        val layoutInflater: LayoutInflater = LayoutInflater.from(mCtx)
        val view : View = layoutInflater.inflate(layoutResId, null)
        val textNama = view.findViewById<TextView>(R.id.tv1)
        val  textStatus = view.findViewById<TextView>(R.id.tv2)
        val textUpdate = view.findViewById<TextView>(R.id.update)
        val textDelete = view.findViewById<TextView>(R.id.delete)

        val user = list[position]
        textNama.text = user.nama
        textStatus.text = user.status
        textUpdate.setOnClickListener {
            showUpdateDialog(user)
        }
        textDelete.setOnClickListener {
            Deleteinfo(user)
        }

        return view
    }
    fun showUpdateDialog(user: Users){
        val builder = AlertDialog.Builder(mCtx)
        builder.setTitle("update")
        val inflater = LayoutInflater.from(mCtx)
        val view = inflater.inflate(R.layout.update_data, null)
        val textNama = view.findViewById<EditText>(R.id.inputNama)
        val textStatus = view.findViewById<EditText>(R.id.inputStatus)
        textNama.setText(user.nama)
        textStatus.setText(user.status)
        builder.setView(view)
        builder.setPositiveButton("Update"){
            dialog, which ->
            val dbUsers = FirebaseDatabase.getInstance().getReference("USERS")
            val nama = textNama.text.toString().trim()
            val status = textStatus.text.toString().trim()
            if (nama.isEmpty()){
                textStatus.error = "Please enter Status"
                textStatus.requestFocus()
                return@setPositiveButton
            }
            val user = Users(user.id, nama, status)
            dbUsers.child(user.id).setValue(user).addOnCompleteListener { 
                Toast.makeText(mCtx, "Updated", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("No"){
            dialog, which ->
        }
        val alert = builder.create()
        alert.show()

    }
    fun Deleteinfo(user: Users){
        val prograssDialog = ProgressDialog(context, R.style.AppTheme)
        prograssDialog.isIndeterminate = true
        prograssDialog.setMessage("Deleting...")
        prograssDialog.show()
        val mydatabase = FirebaseDatabase.getInstance().getReference("USERS")
        mydatabase.child(user.id).removeValue()
        Toast.makeText(mCtx, "Deletd!!", Toast.LENGTH_SHORT).show()
        val intent = Intent(context, ShowData::class.java)
        context.startActivity(intent)
        prograssDialog.dismiss()
    }

}