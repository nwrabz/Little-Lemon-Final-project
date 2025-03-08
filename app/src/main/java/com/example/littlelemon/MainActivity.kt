package com.example.littlelemon

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.littlelemon.ui.theme.LittleLemonTheme
import com.example.littlelemon.ui.theme.Yellow
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(contentType = ContentType("text", "plain"))
        }
    }

    private val database by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "database").build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val databaseMenuItems = remember {
                mutableStateListOf<MenuItemRoom>()
            }
            var orderMenuItems by remember {
                mutableStateOf(true)
            }
            LittleLemonTheme {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "logo",
                        modifier = Modifier.padding(50.dp)
                    )

                    // add Button code here
                    val btn = Button(
                        onClick = {
                            orderMenuItems = true
                        },
                        colors = ButtonDefaults.buttonColors(Yellow),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Tap to Order By Name")
                    }

                    // add searchPhrase variable here
                    var searchPhrase by remember {
                        mutableStateOf(TextFieldValue())
                    }
                    // Add OutlinedTextField
                    OutlinedTextField(
                        value = searchPhrase,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 50.dp),
                        maxLines = 1,
                        onValueChange = {
                            searchPhrase = it
                            if (searchPhrase.text.isNotEmpty()) {
                                orderMenuItems = false
                            }
                        },
                        label = {
                            Text("Search")
                        }
                    )

                    // add is not empty check here
                    MenuItemsList(
                        if (orderMenuItems) databaseMenuItems
                        else if (searchPhrase.text.isNotBlank()) {
                            databaseMenuItems.filter { item ->
                                item.title.lowercase().contains(searchPhrase.text.lowercase())
                            }
                        } else emptyList()
                    )
                }
            }
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    if (database.menuItemDao().isEmpty()) {
                        kotlin.runCatching {
                            val items = fetchMenu()
                            saveMenuToDatabase(items)
                        }.onFailure {
                            Log.e(
                                "MainActivity",
                                "err fetching and saving data",
                                it
                            )
                        }
                    }
                    val dbItems = database.menuItemDao().getAll().sortedBy { it.title }
                    withContext(Dispatchers.Main) {
                        databaseMenuItems.addAll(dbItems)
                    }
                }
            }
        }
    }

    private suspend fun fetchMenu(): List<MenuItemNetwork> {
        val bytes = httpClient.get(
            "https://raw.githubusercontent.com/Meta-Mobile-Developer-PC/Working-With-Data-API/main/littleLemonSimpleMenu.json"
        ).readBytes()
        return Json.Default.decodeFromString<MenuNetwork>(String(bytes))
    }

    private fun saveMenuToDatabase(menuItemsNetwork: List<MenuItemNetwork>) {
        val menuItemsRoom = menuItemsNetwork.map { it.toMenuItemRoom() }
        database.menuItemDao().insertAll(*menuItemsRoom.toTypedArray())
    }
}

@Composable
private fun MenuItemsList(items: List<MenuItemRoom>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .padding(top = 20.dp)
    ) {
        items(
            items = items,
            itemContent = { menuItem ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(menuItem.title)
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(5.dp),
                        textAlign = TextAlign.Right,
                        text = "%.2f".format(menuItem.price)
                    )
                }
            }
        )
    }
}
