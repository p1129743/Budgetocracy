package projet.ift2905.budgetocracy;

import android.Manifest;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import info.hoang8f.android.segmented.SegmentedGroup;

enum typeSort {
    sortByName,
    sortByDate,
    sortByAmount
}

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    final String[] PERMISSIONS = {Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET};
    final int PERMISSION_ALL = 101;
    final int REQUEST_IMAGE_CAPTURE = 102;
    final int REQUEST_EXPENSE_DATA = 103;
    final int REQUEST_BUDGET_DATA = 104;
    final int REQUEST_MODIFICATION_EXPENSE_DATA = 105;
    final int REQUEST_MODIFICATION_BUDGET_DATA = 106;
    public SharedPreferences prefs;
    public EnumSort mSort;
    //RECHERCHE
    ListView mListView;
    TextView mEmptyView;
    Cursor cursor;
    //LISTE BUDGET MAIN
    ListView mListViewBudget;
    Cursor cursorBudget;
    private PieChart pieChart;
    private CustomAdapter customAdapter;
    private RadioButton mButtonDate;
    private RadioButton mButtonAmount;
    private RadioButton mButtonName;
    // INTERFACE
    private BottomNavigationViewEx mBottomBar; // Menu de l'écran principal
    private DBHelper_Expenses DB_Expenses;
    private DBHelper_Budget DB_Budget;
    private TextView navHeaderDate;
    private TextView legend;
    private RelativeLayout groupPieChart;
    private CustomAdapterMainBudget customAdapterBudget;
    private LinearLayout popupNoBudget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        DB_Expenses = new DBHelper_Expenses(this);
        DB_Budget = new DBHelper_Budget(this);

        View view = this.getWindow().getDecorView();
        view.setBackgroundColor(getResources().getColor(R.color.dark_grey));

        // Gère la liste des budgets apparaissant dans le menu principal
        cursorBudget = DB_Budget.getAllData();

        mListViewBudget = findViewById(R.id.lstBudget);
        groupPieChart = findViewById(R.id.groupPieChart);

        mListViewBudget.setBackground(getDrawable(R.drawable.shadow_listview));
        groupPieChart.setBackground(getDrawable(R.drawable.shadow_listview));

        customAdapterBudget = new CustomAdapterMainBudget(this, cursorBudget);
        mListViewBudget.setAdapter((ListAdapter) customAdapterBudget);
        mListViewBudget.setDividerHeight(2);

        // Actions liées au budget (modification, suppression)
        mListViewBudget.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final String strId = String.valueOf(id);
                String[] list = {getResources().getString(R.string.modify_budget), getResources().getString(R.string.delete_budget)};

                final AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setCancelable(true);
                builder.setTitle(((Cursor)parent.getAdapter().getItem(position)).getString(1));
                builder.setItems(list, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: // Modification du budget
                                // On appelle l'activité pour modifier la dépense (qui est en soit l'activité de création de dépense, auxquelle on fourni un extra particulier)
                                Intent modifyExpense = new Intent(MainActivity.this, NewCategoriesActivity.class);
                                modifyExpense.putExtra("requestModifyData", true);
                                modifyExpense.putExtra("idBudgetToModify", strId);
                                startActivityForResult(modifyExpense, REQUEST_MODIFICATION_BUDGET_DATA);
                                updateMainListBudget();
                                pieChartSetup();
                                break;

                            case 1: // Suppression du budget
                                requestBudgetDelete(strId);
                                break;

                            default:
                                break;
                        }
                    }
                });
                builder.show();
                return true;
            }
        });

        // Affiche les dépenses liées au budget selectionné
        mListViewBudget.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String strId = String.valueOf(id);

                // On appelle l'activité pour visualiser la liste des dépenses liée au budget
                Intent seeExpensesRelativeToBudget = new Intent(MainActivity.this, ExpensesRelativeToBudget.class);
                seeExpensesRelativeToBudget.putExtra("idBudget", strId);
                startActivity(seeExpensesRelativeToBudget);
            }
        });


        // Accès à tous les Views
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navHeaderDate = navigationView.getHeaderView(0).findViewById(R.id.nav_date);
        mBottomBar = findViewById(R.id.menuBar);
        mListView = findViewById(R.id.lstExpenses);
        mButtonAmount = findViewById(R.id.buttonSortAmount);
        mButtonDate = findViewById(R.id.buttonSortDate);
        mButtonName = findViewById(R.id.buttonSortName);
        pieChart = findViewById(R.id.piechart_1);
        legend = findViewById(R.id.pieChartLegend);
        popupNoBudget = findViewById(R.id.popupNoBudget);

        try {
            // Checking journalier (reset mensuel/dépenses à mettre à jour)
            dailyUpdate();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        setSupportActionBar(toolbar);

        String currentDate = getCurrentDate(getApplicationContext());
        getSupportActionBar().setTitle(currentDate);

        Calendar cal = Calendar.getInstance();
        if (navHeaderDate != null) {
            navHeaderDate.setText(getDayOfWeek(cal.get(cal.DAY_OF_WEEK), getApplicationContext()) + currentDate);
        }

        // Barre de navigation (en bas)
        mBottomBar.setActivated(true);
        mBottomBar.enableItemShiftingMode(false);
        mBottomBar.enableAnimation(false);
        mBottomBar.enableShiftingMode(false);
        mBottomBar.setTextVisibility(false);

        // Liens d'écoute sur la barre de navigation
        mBottomBar.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_scan:
                        // Si Permission accordée:
                        if (hasPermissions(getApplicationContext(), PERMISSIONS)) {
                            Intent takePhotoIntent = new Intent(MainActivity.this, CameraActivity.class);
                            // Activité Caméra lancée dans l'attente d'une réponse (image)
                            startActivityForResult(takePhotoIntent, REQUEST_IMAGE_CAPTURE);
                            return true;
                        } else {
                            // Sinon: les demander
                            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, PERMISSION_ALL);
                            return false;
                        }

                    case R.id.menu_categories:
                        startActivityForResult(new Intent(MainActivity.this, NewCategoriesActivity.class), REQUEST_BUDGET_DATA);
                        return true;

                    case R.id.menu_add_expenses:
                        startActivityForResult(new Intent(MainActivity.this, NewExpensesActivity.class), REQUEST_EXPENSE_DATA);
                        return true;

                    case R.id.menu_graphiques:
                        startActivity(new Intent(MainActivity.this, GraphicActivity.class));
                        return true;
                    default:
                        return false;
                }
            }
        });

        // Affichage du menu latéral gauche
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        /**** MAIN GRAPH ****/
        pieChartSetup();
    }

    // Reactualise l'affichage de la liste de Budgets quand on revient à la mainActivity
    @Override
    protected void onResume() {
        super.onResume();
        updateMainListBudget();
        pieChartSetup();
    }

    private void pieChartSetup() {
        pieChart.setRotationEnabled(true);
        pieChart.setUsePercentValues(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setHoleRadius(85.f);
        pieChart.setTransparentCircleRadius(90f);
        pieChart.setDrawCenterText(true);

        // Disable the legend
        Legend l = pieChart.getLegend();
        l.setEnabled(false);

        // Text from the middle of the piechart graph
        float budget = 0;
        float remaining = 0;
        float usedBudget;
        int nbDepenses;
        DecimalFormat df = new DecimalFormat("0.00");

        cursor = DB_Expenses.getAllData();
        nbDepenses = cursor.getCount();

        cursor = DB_Budget.getAllData();
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            do {
                budget += Float.parseFloat(cursor.getString(2));
                remaining += Float.parseFloat(cursor.getString(3));
            } while (cursor.moveToNext());
        }

        usedBudget = budget - remaining + 0.0f;
        String currency = prefs.getString("currency", "$");

        SpannableString s = new SpannableString(getString(R.string.epargne) + "\n" + df.format(remaining) + currency);
        s.setSpan(new RelativeSizeSpan(1.5f), getString(R.string.epargne).length(), s.length(), 0);
        //s.setSpan(new ForegroundColorSpan(Color.rgb(16,176,115)),s.length()-1-Float.toString(remaining).length(), s.length(), 0);
        pieChart.setCenterText(s);

        // Add the values to the piechart
        ArrayList<PieEntry> yValues = new ArrayList<>();
        yValues.add(new PieEntry(usedBudget, ""));
        yValues.add(new PieEntry(remaining, ""));
        PieDataSet dataSet = new PieDataSet(yValues, "Budget");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setValueTextSize(0f);

        // Piechart bar colors
        ArrayList colors = new ArrayList();
        colors.add(0xFFFFFFFF);
        colors.add(0xFF10B073);
        dataSet.setColors(colors);

        PieData data = new PieData((dataSet));
        pieChart.setData(data);

        //Legend
        String string = getString(R.string.totalbudget) + "\n" + df.format(budget) + currency
                + "\n" + getString(R.string.used)
                + "\n" + df.format(usedBudget) + currency
                + "\n" + getString(R.string.nbBudget)
                + "\n" + cursor.getCount()
                + "\n" + getString(R.string.nbExpenses)
                + "\n" + nbDepenses;
        int counter;
        int counter2;
        SpannableString string2 = new SpannableString(string);
        counter = getString(R.string.totalbudget).length();
        string2.setSpan(new StyleSpan(Typeface.BOLD), 0, counter, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        counter2 = counter + (df.format(budget) + currency).length() + 1;
        string2.setSpan(new RelativeSizeSpan(1.4f), counter, counter2, 0);
        counter = counter2 + getString(R.string.used).length() + 2;
        string2.setSpan(new StyleSpan(Typeface.BOLD), counter2, counter, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        counter2 = counter + (df.format(usedBudget) + currency).length();
        string2.setSpan(new RelativeSizeSpan(1.4f), counter, counter2, 0);
        counter = counter2 + getString(R.string.nbBudget).length() + 1;
        string2.setSpan(new StyleSpan(Typeface.BOLD), counter2, counter, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        counter2 = counter + Float.toString(cursor.getCount()).length();
        string2.setSpan(new RelativeSizeSpan(1.4f), counter, counter2, 0);
        counter = string.length() - Float.toString(nbDepenses).length() + 1;
        string2.setSpan(new StyleSpan(Typeface.BOLD), counter2, counter, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        string2.setSpan(new RelativeSizeSpan(1.4f), string.length() - Float.toString(nbDepenses).length() + 1, string.length(), 0);

        legend.setText(string2);
    }

    // Récupère les données attendues d'une activité selon le "requestCode"
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                if (resultCode == RESULT_OK) {
                    String photoBase64 = data.getStringExtra("photoBase64");
                    Intent addExpenseWithData = new Intent(MainActivity.this, NewExpensesActivity.class);
                    addExpenseWithData.putExtra("requestDataToAPI", true);
                    addExpenseWithData.putExtra("photoBase64", photoBase64);
                    startActivity(addExpenseWithData);
                }
                break;

            case REQUEST_EXPENSE_DATA:
                if (resultCode == RESULT_OK && data != null) {
                    String[] dataToAdd = data.getStringArrayExtra("dataToSave");

                    Integer budgetID = Integer.valueOf(dataToAdd[1]);
                    System.out.println("dataToAdd3 = " + dataToAdd[3]);
                    String expenseMonth = dataToAdd[3].split("-")[1];

                    if (isSameMonthAsCurrent(expenseMonth)) {
                        DB_Budget.substractRemainingAmount(budgetID, Float.valueOf(dataToAdd[2]));
                    }
                    DB_Expenses.insertDataName(dataToAdd[0], budgetID, Float.valueOf(dataToAdd[2]), dataToAdd[3]);
                    Toast.makeText(getApplicationContext(), R.string.successful_expense_add, Toast.LENGTH_LONG).show();
                }
                break;


            case REQUEST_MODIFICATION_EXPENSE_DATA:
                if (resultCode == RESULT_OK && data != null) {
                    String[] dataToModify = data.getStringArrayExtra("dataToSave");
                    String idExpense = data.getStringExtra("idExpenseToModify");
                    Float oldExpenseValue = data.getFloatExtra("oldExpenseValue", 0);

                    String expenseMonth = dataToModify[3].split("-")[1];
                    Integer categoryID = Integer.valueOf(dataToModify[1]);

                    if (isSameMonthAsCurrent(expenseMonth)) {
                        DB_Budget.increaseRemainingAmount(categoryID, oldExpenseValue);
                        DB_Budget.substractRemainingAmount(categoryID, Float.valueOf(dataToModify[2]));
                    }
                    DB_Expenses.updateData(idExpense, dataToModify[0], categoryID, Float.valueOf(dataToModify[2]), dataToModify[3]);
                    Toast.makeText(getApplicationContext(), R.string.successful_expense_modification, Toast.LENGTH_LONG).show();

                }
                break;

            case REQUEST_BUDGET_DATA:
                if (resultCode == RESULT_OK && data != null) {
                    String[] dataToAdd = data.getStringArrayExtra("dataToModify");
                    Float budgetValue = Float.valueOf(dataToAdd[1]);

                    DB_Budget.insertDataName(dataToAdd[0], budgetValue, budgetValue);
                    Toast.makeText(getApplicationContext(), R.string.successful_category_add, Toast.LENGTH_LONG).show();
                }
                break;

            case REQUEST_MODIFICATION_BUDGET_DATA:
                if (resultCode == RESULT_OK && data != null) {
                    String[] dataToModify = data.getStringArrayExtra("dataToModify");
                    String idExpense = data.getStringExtra("idBudgetToModify");
                    Float oldBudgetValue = Float.valueOf(data.getStringExtra("oldBudgetValue"));
                    Float newBudgetValue = Float.valueOf(dataToModify[1]);
                    Float remaining = DB_Budget.getRemaining(idExpense);

                    Float newRemaining = newBudgetValue - (oldBudgetValue - remaining);

                    // Calcul du nouveau remaining (selon la différence entre l'ancienne valeur et la nouvelle)
                    DB_Budget.updateData(idExpense, dataToModify[0], Float.valueOf(dataToModify[1]), newRemaining);
                    Toast.makeText(getApplicationContext(), R.string.successful_category_modified, Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    public void dailyUpdate() throws ParseException {
        SharedPreferences.Editor edit = prefs.edit();
        Calendar cal = Calendar.getInstance();

        Calendar nextMonth = Calendar.getInstance();
        nextMonth.add(Calendar.MONTH, 1); // Prochain mois

        DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        DateFormat monthyear = new SimpleDateFormat("MM-yyyy");

        String today = df.format(cal.getTime());
        String resetDay = "01-" + monthyear.format(nextMonth.getTime());

        // Si le jour de reset prochain n'est pas défini, l'ajouter dans les préférences
        if (!prefs.contains("nextResetDate")) {
            edit.putString("nextResetDate", resetDay);
            edit.apply();
        }
        String nextResetDate = prefs.getString("nextResetDate", resetDay);

        Date dToday = df.parse(today);
        Date dNextResetDay = df.parse(nextResetDate);

        if (dToday.compareTo(dNextResetDay) > 0) { // Si le jour courant dépasse la date de reset, alors faire le reset et calculer la prochaine date de reset
            DB_Budget.resetRemaining();
            DB_Budget.updateRemaining(DB_Expenses.getAllData());
            updateMainListBudget();
            Snackbar.make(findViewById(R.id.myCoordinatorLayout), R.string.monthly_reset_process, Snackbar.LENGTH_LONG).show();

            // Défini la prochaine date de reset
            edit.putString("nextResetDate", "01-" + monthyear.format(nextMonth.getTime()));
            edit.apply();
        }
    }

    @Override
    // Gère la recherche
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView mSearchView = (SearchView) menu.findItem(R.id.search).getActionView();
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        // Retire la barre en dessous de la recherche
        int searchPlateId = mSearchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
        View searchPlateView = mSearchView.findViewById(searchPlateId);

        SegmentedGroup mSegGroup = findViewById(R.id.segGroupResearch);
        mSegGroup.setTintColor(getResources().getColor(R.color.colorPrimary));

        mSearchView.setQueryHint("Recherche");
        if (searchPlateView != null) {
            searchPlateView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }

        mSearchView.setQueryHint(getResources().getString(R.string.research));
        customAdapter = new CustomAdapter(this, cursor);

        mListView.setAdapter((ListAdapter) customAdapter);
        mListView.setEmptyView(mEmptyView);

        setResearchVisibility(false); // Cache les elements de recherche

        mSort = new EnumSort(typeSort.sortByName);
        mButtonName.setChecked(true);

        // On entre dans la recherche : Gestion recherche + Trie selon le bouton choisi
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String query) {
                cursor = DB_Expenses.getExpenseListByKeyword(query, mSort);

                if (cursor == null) {
                    Toast.makeText(MainActivity.this, "Pas de dépense trouvée!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, cursor.getCount() + " dépenses trouvée.s!", Toast.LENGTH_LONG).show();
                }
                customAdapter.changeCursor(cursor);

                mButtonAmount.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSort.changeSort(typeSort.sortByAmount);
                        updateResearchList(query);
                    }
                });

                mButtonDate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSort.changeSort(typeSort.sortByDate);
                        updateResearchList(query);
                    }
                });

                mButtonName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSort.changeSort(typeSort.sortByName);
                        updateResearchList(query);
                    }
                });
                return false;
            }

            @Override
            public boolean onQueryTextChange(final String newText) {
                setResearchVisibility(true);
                setMainViewVisibility(false);

                cursor = DB_Expenses.getExpenseListByKeyword(newText, mSort);

                customAdapter.changeCursor(cursor);
                if (cursor != null) {
                    customAdapter.changeCursor(cursor);
                }

                mButtonAmount.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSort.changeSort(typeSort.sortByAmount);
                        updateResearchList(newText);
                    }
                });

                mButtonDate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSort.changeSort(typeSort.sortByDate);
                        updateResearchList(newText);
                    }
                });

                mButtonName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSort.changeSort(typeSort.sortByName);
                        updateResearchList(newText);
                    }
                });
                return false;
            }
        });

        mSearchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateResearchList("");
                // Affiche l'interface de recherche
                setResearchVisibility(true);
                // Cache l'écran principal
                setMainViewVisibility(false);
            }
        });
        // Quand on quitte la recherche on fait disparaitre le ListView
        mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                // Cache l'interface de recherche
                setResearchVisibility(false);
                // Réactivation ecran principal
                setMainViewVisibility(true);
                return false;
            }
        });

        // Quand on clique sur une des dépenses de la liste, on redirige vers la page pour la modifier
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String strId = String.valueOf(id);
                String[] list = {getResources().getString(R.string.modify), getResources().getString(R.string.delete)};

                final AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setCancelable(true);
                builder.setTitle(((Cursor)parent.getAdapter().getItem(position)).getString(1));
                builder.setIcon(R.drawable.ic_edit_black_24dp);
                builder.setItems(list, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: // Modification
                                modifyExpenseOptions(strId);
                                // Une fois qu'on a modifié la dépense, on ferme la recherche précédente pour l'actualiser
                                mSearchView.setQuery("", false);
                                mSearchView.setIconified(true);
                                break;

                            case 1: // Suppression
                                deleteExpenseOptions(strId);

                                // Refresh la liste de dépenses dans la recherche
                                updateResearchList("");

                                // Refresh la liste de budgets
                                updateMainListBudget();
                                pieChartSetup();
                                break;

                            default:
                                break;
                        }
                    }
                });
                builder.show();
            }
        });
        return true;
    }

    public void modifyExpenseOptions(String strId) {
        // On appelle l'activité pour modifier la dépense (qui est en soit l'activité de création de dépense, auxquelle on fourni un extra particulier)
        Intent modifyExpense = new Intent(MainActivity.this, NewExpensesActivity.class);
        modifyExpense.putExtra("requestModifyData", true);
        modifyExpense.putExtra("idExpenseToModify", strId);
        startActivityForResult(modifyExpense, REQUEST_MODIFICATION_EXPENSE_DATA);
    }

    public void deleteExpenseOptions(String strId) {
        Cursor expenseData = DB_Expenses.getExpense(strId); // Toutes les informations de la dépense sélectionné

        if (expenseData != null) {
            String expenseDate = expenseData.getString(4).split("-")[1];
            if (isSameMonthAsCurrent(expenseDate)) {
                // Mets à jour le reste du budget associé à la dépense si cette dernière et enregistré pour le mois courant
                DB_Budget.increaseRemainingAmount(expenseData.getInt(2), expenseData.getFloat(3));
            }
            DB_Expenses.deleteData(strId);
            Toast.makeText(getApplicationContext(), R.string.successful_expense_deleted, Toast.LENGTH_SHORT).show();
        }
    }

    public void updateMainListBudget() {
        cursorBudget = DB_Budget.getAllData();
        customAdapterBudget.changeCursor(cursorBudget);
        // Vérifier si vide : si oui affichage message
        if(cursorBudget.getCount() == 0){
            popupNoBudget.setVisibility(View.VISIBLE);

        }
        else {
            popupNoBudget.setVisibility(View.GONE);
        }
    }

    public void updateResearchList(String search) {
        cursor = DB_Expenses.getExpenseListByKeyword(search, mSort);
        customAdapter.changeCursor(cursor);
    }

    public void setResearchVisibility(boolean shown) {
        int visibility = shown ? View.VISIBLE : View.GONE;

        mListView.setVisibility(visibility);
        mButtonDate.setVisibility(visibility);
        mButtonAmount.setVisibility(visibility);
        mButtonName.setVisibility(visibility);

        if (shown) {
            this.getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.colorMainBackground));
            mListView.setDivider(null);
            mListView.setDividerHeight(2);
        }
    }

    public void setMainViewVisibility(boolean shown) {
        int visibility = shown ? View.VISIBLE : View.GONE;

        mBottomBar.setVisibility(visibility);
        mListViewBudget.setVisibility(visibility);
        pieChart.setVisibility(visibility);
        legend.setVisibility(visibility);

        if(cursorBudget.getCount() == 0){
            popupNoBudget.setVisibility(visibility);
        }

        if (shown) {
            this.getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.dark_grey));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.currency_settings:
                final String[] listCurrencies = {"$ - " + getString(R.string.CAD),
                        "€ - " + getString(R.string.EUR),
                        "$ - " + getString(R.string.USD),
                        "$ - " + getString(R.string.AUD),
                        "£ - " + getString(R.string.GBP),
                        "Fr. - " + getString(R.string.CHF),
                        "¥ - " + getString(R.string.JPY),
                        "R - " + getString(R.string.ZAR),
                };

                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setCancelable(true);
                builder.setTitle(R.string.pick_currency);
                builder.setItems(listCurrencies, null);

                int currentChoice = prefs.getInt("currencyID", 0);

                builder.setSingleChoiceItems(listCurrencies, currentChoice, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String chosenCurrency = listCurrencies[which].split("-")[0].trim();
                        SharedPreferences.Editor editor = prefs.edit();

                        editor.putString("currency", chosenCurrency);
                        editor.putInt("currencyID", which); // Modification du paramètre "devise"
                        editor.apply();
                        pieChartSetup();
                        Toast.makeText(getApplicationContext(), "Devise choisie : " + chosenCurrency, Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    }
                });

                builder.setNegativeButton(R.string.cancel, null);
                builder.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_erase_data:
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setCancelable(true);
                builder.setTitle(R.string.erase_all_data);
                builder.setMessage(R.string.erase_all_data_message);
                builder.setNegativeButton(R.string.cancel, null);
                builder.setPositiveButton(R.string.erase_all, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DB_Expenses.deleteDataBase();
                        DB_Budget.deleteDataBase();
                        updateMainListBudget();
                        Snackbar.make(findViewById(R.id.myCoordinatorLayout), R.string.successful_erased_data, Snackbar.LENGTH_SHORT).show();
                        pieChartSetup();
                    }
                });
                builder.show();
                break;

            case R.id.nav_communicate:
                Snackbar.make(findViewById(R.id.myCoordinatorLayout), "Pas implémenté car pas nécéssaire", Snackbar.LENGTH_LONG).show();
                break;

            case R.id.nav_donation:
                Snackbar.make(findViewById(R.id.myCoordinatorLayout), "Pas implémenté, nous ne sommes pas des escrocs", Snackbar.LENGTH_LONG).show();
                break;

            case R.id.nav_credit:
                final AlertDialog.Builder builderCredit = new AlertDialog.Builder(this);
                builderCredit.setCancelable(true);
                builderCredit.setTitle(R.string.credit);
                builderCredit.setMessage(R.string.icon_credit);
                builderCredit.setPositiveButton(R.string.OK, null);
                builderCredit.show();
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // Gère la suppression de budget
    public void requestBudgetDelete(final String budgetID) {
        Cursor expensesOfBudget = DB_Expenses.getExpensesAssociateToBudget(budgetID); // Toutes les informations du budget sélectionné
        if (expensesOfBudget != null) {
            if (expensesOfBudget.getCount() > 0) {
                // Afficher un message avisant l'utilisateur de la suppression des dépenses associées
                final AlertDialog.Builder message = new AlertDialog.Builder(MainActivity.this);
                message.setCancelable(true);
                message.setMessage(R.string.existing_expenses_warning);
                message.setIcon(R.drawable.ic_delete_black_24dp);
                message.setTitle(R.string.existing_expenses_warning_title);
                message.setNegativeButton(R.string.cancel, null);
                message.setPositiveButton(R.string.delete_anyway, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DB_Budget.deleteData(budgetID);
                        DB_Expenses.deleteExpensesOfBudget(budgetID);
                        updateMainListBudget();
                        pieChartSetup();
                        Snackbar.make(findViewById(R.id.myCoordinatorLayout), R.string.successful_erased_data, Snackbar.LENGTH_SHORT).show();
                    }
                });
                message.show();
            }
        } else {
            // Afficher un message avisant l'utilisateur de la suppression des dépenses associées
            final AlertDialog.Builder message = new AlertDialog.Builder(MainActivity.this);
            message.setCancelable(true);
            message.setMessage(R.string.delete_budget_msg);
            message.setIcon(R.drawable.ic_delete_black_24dp);
            message.setTitle(R.string.delete_budget);
            message.setNegativeButton(R.string.cancel, null);
            message.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DB_Budget.deleteData(budgetID);
                    updateMainListBudget();
                    pieChartSetup();
                    Snackbar.make(findViewById(R.id.myCoordinatorLayout), R.string.successful_budget_deleted, Snackbar.LENGTH_SHORT).show();
                }
            });
            message.show();
        }
    }

    // Affiche la date courante dans le Toolbar
    public static String getCurrentDate(Context ctx) {
        DateFormat dateFormat = new SimpleDateFormat("d-MM-yyyy");
        Date date = new Date();
        String[] dateStr = dateFormat.format(date).split("-");

        return dateStr[0] + getMonthFromNb(dateStr[1], ctx) + dateStr[2];
    }

    public static String getMonthFromNb(String nb, Context ctx) {
        switch (nb) {
            case "01":
                return " " + ctx.getString(R.string.janvier) + " ";
            case "02":
                return " " + ctx.getString(R.string.fevrier) + " ";
            case "03":
                return " " + ctx.getString(R.string.mars) + " ";
            case "04":
                return " " + ctx.getString(R.string.avril) + " ";
            case "05":
                return " " + ctx.getString(R.string.mai) + " ";
            case "06":
                return " " + ctx.getString(R.string.juin) + " ";
            case "07":
                return " " + ctx.getString(R.string.juillet) + " ";
            case "08":
                return " " + ctx.getString(R.string.aout) + " ";
            case "09":
                return " " + ctx.getString(R.string.septembre) + " ";
            case "10":
                return " " + ctx.getString(R.string.octobre) + " ";
            case "11":
                return " " + ctx.getString(R.string.novembre) + " ";
            case "12":
                return " " + ctx.getString(R.string.decembre) + " ";
            default:
                return " Cinglinglin ";
        }
    }

    public static String getDayOfWeek(int nb, Context ctx) {
        switch (nb) {
            case 1:
                return " " + ctx.getString(R.string.dimanche) + " ";
            case 2:
                return " " + ctx.getString(R.string.lundi) + " ";
            case 3:
                return " " + ctx.getString(R.string.mardi) + " ";
            case 4:
                return " " + ctx.getString(R.string.mercredi) + " ";
            case 5:
                return " " + ctx.getString(R.string.jeudi) + " ";
            case 6:
                return " " + ctx.getString(R.string.vendredi) + " ";
            case 7:
                return " " + ctx.getString(R.string.samedi) + " ";
            default:
                return "Error";
        }
    }

    public static boolean isSameMonthAsCurrent(String month) {
        Calendar cal = Calendar.getInstance();
        DateFormat patternYear = new SimpleDateFormat("M");
        String currentMonth = patternYear.format(cal.getTime());
        if (currentMonth.equals(NewExpensesActivity.fixDate(month))) {
            return true;
        }
        return false;
    }

    /*****************
     *  PERMISSIONS  *
     *****************/
    // Gère la réponse d'un utilisateur à une requête de permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ALL:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && isAllGranted(grantResults)) {
                    // Permission accordée : accès à l'appareil photo
                    startActivity(new Intent(MainActivity.this, CameraActivity.class));
                } else {
                    // Permission refusée: impossible de prendre de photo
                    Toast.makeText(getApplicationContext(), "Permission refusée: impossible d'accéder à l'appareil photo.", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    // Vérification si toutes les permissions demandées ont été acceptés
    public boolean isAllGranted(int[] grantResults) {
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }


    //Vérification si les permissions actuelles sont suffisantes
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}

class EnumSort {
    typeSort sort;

    public EnumSort(typeSort sort) {
        this.sort = sort;
    }

    public void changeSort(typeSort newSort) {
        this.sort = newSort;
    }

    public typeSort getSort() {
        return sort;
    }

}
