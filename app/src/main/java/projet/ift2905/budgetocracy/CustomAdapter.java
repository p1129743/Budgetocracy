package projet.ift2905.budgetocracy;

import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

/**
 * Created by Saîmon on 25/03/2018.
 */

public class CustomAdapter extends CursorAdapter{
    TextView mExpense;
    TextView mCategorie;
    TextView mAmount;
    TextView mDate;


    public CustomAdapter (Context context, Cursor c){
        super(context,c);
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent){
        return LayoutInflater.from(context).inflate(R.layout.item_research,parent,false);
    }


    @Override
    public void bindView(View view, Context context, Cursor cursor){
        mExpense = (TextView) view.findViewById(R.id.txtName);
        mCategorie = (TextView) view.findViewById(R.id.txtCategorie);
        mAmount = (TextView) view.findViewById(R.id.txtAmount);
        mDate = (TextView) view.findViewById(R.id.txtDate);

        DBHelper_Budget DB_Budget;
        DB_Budget = new DBHelper_Budget(context);


        String name = cursor.getString(1);
        String categorie = DB_Budget.getStringBudgetWithID(cursor.getString(2));
        String amount = cursor.getString(3);
        String date = cursor.getString(4);

        amount = "-$ "+amount;

        mExpense.setText(name);
        mCategorie.setText(categorie);
        mAmount.setText(amount);
        mDate.setText(date);

    }







}
