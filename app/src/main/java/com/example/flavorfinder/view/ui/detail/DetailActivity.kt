package com.example.flavorfinder.view.ui.detail

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.flavorfinder.R
import com.example.flavorfinder.databinding.ActivityDetailBinding
import com.example.flavorfinder.di.Injection
import com.example.flavorfinder.helper.Result
import com.example.flavorfinder.helper.ViewModelFactory
import com.example.flavorfinder.network.repository.MealRepository
import com.example.flavorfinder.network.response.GetBookmarkRecipe
import com.example.flavorfinder.network.response.MealsItem
import com.example.flavorfinder.pref.UserPreference
import com.example.flavorfinder.pref.dataStore
import com.example.flavorfinder.view.ui.adapter.CommentListAdapter
import com.example.flavorfinder.view.ui.adapter.IngredientAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailActivity : AppCompatActivity(), CommentListAdapter.OnItemClickCallback {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var commentListAdapter: CommentListAdapter
    private val viewModel by viewModels<DetailViewModel> {
        ViewModelFactory.getInstance(this)
    }
    private lateinit var repository: MealRepository
    private lateinit var userPreference: UserPreference
    private var recipeId: Int? = 0
    private var isBookmarked = false
    private var bookmarkId: String? = null
    private lateinit var currentUserId: String

    private lateinit var btnConfirm: Button
    private lateinit var btnCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        userPreference = UserPreference.getInstance(dataStore)
        repository = Injection.provideRepository(this)

        val mealItem = intent.getParcelableExtra<MealsItem>("data")
        val bookmarkRecipe = intent.getParcelableExtra<GetBookmarkRecipe>("bookmark")

        if (mealItem != null) {
            recipeId = mealItem.idMeal?.toInt()
            setupDetailMenu(mealItem)
            checkIfBookmarked(recipeId!!)

            CoroutineScope(Dispatchers.Main).launch {
                currentUserId = repository.getSession().first().userId
                setupCommentsRecyclerView(recipeId.toString())
            }
            binding.commentEditText.setPostButtonClickListener {
                postComment()
            }


        } else if (bookmarkRecipe != null) {
            recipeId = bookmarkRecipe.idMeal?.toInt()
            fetchAndSetupDetailMenu(bookmarkRecipe.idMeal)
            checkIfBookmarked(bookmarkRecipe.idMeal?.toInt()!!)

            CoroutineScope(Dispatchers.Main).launch {
                currentUserId = repository.getSession().first().userId
                setupCommentsRecyclerView(recipeId.toString())
            }
            binding.commentEditText.setPostButtonClickListener {
                postComment()
            }
        } else {
            showToast("Failed to load data")
        }

        observeBookmark()
        observeComment()

    }

    private fun observeBookmark() {
        viewModel.bookmarkResult.observe(this) { result ->
            when (result) {
                is Result.Succes -> {
                    isBookmarked = !isBookmarked
                    invalidateOptionsMenu()
                    showToast(result.data)
                    showLoading(false)
                }
                is Result.Error -> {
                    result.error
                    showLoading(false)
                }
                is Result.Loading -> showLoading(true)
            }
        }

        viewModel.bookmarkId.observe(this) { bookmarkId ->
            Log.d("DetailActivity", "Bookmark ID updates: $bookmarkId")
            this.bookmarkId = bookmarkId
        }
    }

    private fun observeComment() {
        viewModel.commentResult.observe(this) { result ->
            when (result) {
                is Result.Succes -> {
                    showToast("Comment added successfully")
                    showLoadingComment(false)
                }
                is Result.Error -> {
                    showToast("Failed to add comment: ${result.error}")
                    Log.d(result.error, "failed to add comment")
                    showLoadingComment(false)
                }
                is Result.Loading -> { showLoadingComment(true) }
            }
        }

        viewModel.commentsWithUserProfiles.observe(this) { comments ->
            commentListAdapter.submitList(comments)
        }

    }

    private fun fetchAndSetupDetailMenu(mealId: String?) {
        if (mealId == null) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = repository.lookupMeals(mealId)
                val mealItem = response.meals.firstOrNull()
                if (mealItem != null) {
                    withContext(Dispatchers.Main) {
                        setupDetailMenu(mealItem)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showToast("Meal details not found")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Lookup meal details failed")
                }
            }
        }
    }

    private fun setupDetailMenu(items: MealsItem) {
        if (isDestroyed) return

        binding.apply {
            tvMenuName.text = items.strMeal
            if (!this@DetailActivity.isDestroyed) {
                Glide.with(this@DetailActivity)
                    .load(items.strMealThumb)
                    .into(ivDetail)
            }

            val ingredients = listOf(
                items.strMeasure1 + " " + items.strIngredient1, items.strMeasure2 + " " + items.strIngredient2, items.strMeasure3 + " " + items.strIngredient3, items.strMeasure4 + " " + items.strIngredient4,
                items.strMeasure5 + " " + items.strIngredient5, items.strMeasure6 + " " + items.strIngredient6, items.strMeasure7 + " " + items.strIngredient7, items.strMeasure8 + " " + items.strIngredient8,
                items.strMeasure9 + " " + items.strIngredient9, items.strMeasure10 + " " + items.strIngredient10, items.strMeasure11 + " " + items.strIngredient11, items.strMeasure12 + " " + items.strIngredient12,
                items.strMeasure13 + " " + items.strIngredient13, items.strMeasure14 + " " + items.strIngredient14, items.strMeasure15 + " " + items.strIngredient15, items.strMeasure16 + " " + items.strIngredient16,
                items.strMeasure17 + " " + items.strIngredient17, items.strMeasure18 + " " + items.strIngredient18, items.strMeasure19 + " " + items.strIngredient19, items.strMeasure20 + " " + items.strIngredient20
            ).filter { it.isNotBlank() && !it.contains("null null") && !it.contains("null") }

            binding.rvIngredient.layoutManager = LinearLayoutManager(this@DetailActivity)
            binding.rvIngredient.adapter = IngredientAdapter(ingredients)

            tvInstruction.text = items.strInstructions
            tvCategory.text = items.strCategory
            tvCountry.text = items.strArea
        }
    }

    private fun postComment() {
        val commentText = binding.commentEditText.text.toString().trim()
        if (commentText.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                val userId = repository.getSession().first().userId
                viewModel.addComment(recipeId.toString(), userId, commentText)
                viewModel.getComments(recipeId.toString())
            }
        } else {
            showToast("Comment cannot be empty")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_detail, menu)
        val bookmarkItem = menu?.findItem(R.id.action_bookmark)
        bookmarkItem?.setIcon(if (isBookmarked) R.drawable.ic_bookmarked_circle else R.drawable.ic_bookmark_circle_background)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_bookmark -> {
                Log.d("DetailActivity", "Bookmark clicked. isBookmarked: $isBookmarked, bookmarkId: $bookmarkId")
                if (isBookmarked) {
                    bookmarkId?.let {
                        viewModel.deleteBookmark(it)
                    } ?: showToast("Bookmark ID is not available")
                } else {
                    recipeId?.let {
                        viewModel.addBookmark(it)
                    } ?: showToast("Recipe ID is not available")
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkIfBookmarked(recipeId: Int) {
        viewModel.checkIfBookmarked(recipeId).observe(this) { result ->
            when (result) {
                is Result.Succes -> {
                    isBookmarked = result.data != null
                    bookmarkId = result.data
                    invalidateOptionsMenu()
                }
                is Result.Error -> result.error
                is Result.Loading -> showToast("Loading..")
            }
        }
    }

    private fun setupCommentsRecyclerView(recipeId: String) {
        commentListAdapter = CommentListAdapter(this, currentUserId)
        binding.rvComment.apply {
            layoutManager = LinearLayoutManager(this@DetailActivity).apply {
                reverseLayout = true
                stackFromEnd = true
            }
            adapter = commentListAdapter
        }

        viewModel.getComments(recipeId)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onButtonClick(position: Int) {
        val comments = viewModel.commentsWithUserProfiles.value?.toMutableList()
        if (comments != null && position >= 0 && position < comments.size) {
            val comment = comments[position]
            showDialogDeleteComment(comment.comment.commentId, comment.comment.recipeId)
            commentListAdapter.submitList(comments)
            observeComment()
        }
    }

    private fun showDialogDeleteComment(commentId: String, recipeId: String) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.custom_dialog_confirm)
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(getDrawable(R.drawable.custom_dialog_bg))
        dialog.setCancelable(false)

        btnConfirm = dialog.findViewById(R.id.btn_confirm)
        btnCancel = dialog.findViewById(R.id.btn_cancel)

        dialog.findViewById<TextView>(R.id.tv_titile_dialog).text =
            getString(R.string.confirm_delete)
        dialog.findViewById<TextView>(R.id.tv_message).text =
            getString(R.string.message_confirm_delete_comment)

        btnConfirm.text = getString(R.string.delete)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            viewModel.deleteComment(commentId)
            viewModel.deleteCommentStatus.observe(this) { result ->
                when (result) {
                    is Result.Succes -> {
                        showToast("Comment deleted!")
                        viewModel.getComments(recipeId)
                    }
                    is Result.Error -> {
                        showToast("Failed to delete comment")
                    }
                    is Result.Loading -> {
                        showLoading(true)
                    }
                }
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showLoadingComment(isLoading: Boolean) {
        binding.progressBarComment.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBarComment.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        observeComment()
    }
}
