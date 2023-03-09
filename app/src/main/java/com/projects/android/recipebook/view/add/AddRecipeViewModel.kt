package com.projects.android.recipebook.view.add

import android.content.Context
import android.os.FileUtils
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.projects.android.recipebook.database.RecipeBookRepository
import com.projects.android.recipebook.databinding.FragmentAddRecipeBinding
import com.projects.android.recipebook.databinding.ItemAddIngredientBinding
import com.projects.android.recipebook.model.enums.Course
import com.projects.android.recipebook.model.enums.PreparationTime
import com.projects.android.recipebook.model.enums.UnitOfMeasure
import com.projects.android.recipebook.utils.PictureUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream

class AddRecipeViewModel(recipeID: Int) : ViewModel() {

	private val recipeBookRepository = RecipeBookRepository.get()

	private val _state: MutableStateFlow<AddRecipeState?> = MutableStateFlow(null)
	val state: StateFlow<AddRecipeState?>
		get() = _state.asStateFlow()

	init {
		viewModelScope.launch {
			// If the navigation comes from SingleRecipe, enable the edit mode
			if (recipeID >= 0) {
				recipeBookRepository.getSingleRecipe(recipeID).collect { recipe ->
					_state.value = AddRecipeState().also {
						it.editMode = true
						it.id = recipe.id
						it.name = recipe.name
						it.portions = recipe.portions
						it.isVeg = recipe.isVeg
						it.isCooked = recipe.isCooked
						it.course = recipe.course
						it.preparationTime = recipe.preparationTime
						it.preparation = recipe.preparation
						it.ingredientsList = recipe.ingredientsList
						it.photoFileName = recipe.photoFileName
					}
				}
			} else {
				// Default values
				_state.value = AddRecipeState().also {
					it.course = Course.SECOND
					it.preparationTime = PreparationTime.THIRTY_MIN
					it.isVeg = true
					it.isCooked = true
					it.portions = 1.toString()
					it.unitIngredient = UnitOfMeasure.GRAM
					it.ingredientsList = mutableListOf()
				}
			}
		}
	}

	fun updateState(onUpdate: (AddRecipeState) -> Unit) {
		_state.update {
			it?.also {
				onUpdate(it)
			}
		}
	}

	fun checkRecipe(binding: FragmentAddRecipeBinding, bindingIngredientsList: MutableList<ItemAddIngredientBinding?>): Boolean {
		return _state.value!!.checkRecipe(binding, bindingIngredientsList)
	}

	fun saveRecipe(context: Context) {
		super.onCleared()
		if (!_state.value?.canceled!!) {
			_state.value?.formatRecipe()
			_state.value?.photoFileName?.let {
				// delete previous picture
				_state.value?.photoFileNamePrevious?.let { photoFileNamePrevious ->
					if (PictureUtils.createPicture(context, photoFileNamePrevious).delete().not()) {
						Toast.makeText(
							context, "Fail to delete previous picture", Toast.LENGTH_SHORT
						).show()
					}
				}
				// save new picture
				FileUtils.copy(FileInputStream(PictureUtils.getCachedPicture(context, it)), FileOutputStream(PictureUtils.createPicture(context, it)))
				// delete temp file
				PictureUtils.getCachedPicture(context, it).delete()
			}
			if (state.value!!.editMode) {
				_state.value?.let { state -> recipeBookRepository.updateRecipe(state.toRecipe()) }
			} else {
				_state.value?.let { state -> recipeBookRepository.insertRecipe(state.toRecipe()) }
			}
		}
	}
}

class AddRecipeViewModelFactory(private val recipeID: Int) : ViewModelProvider.Factory {

	override fun <T : ViewModel> create(modelClass: Class<T>): T = AddRecipeViewModel(recipeID) as T
}