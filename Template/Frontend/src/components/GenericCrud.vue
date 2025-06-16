<template>
  <div class="generic-crud-container">
    <h1>{{ title }}</h1>

    <!-- Formulaire générique -->
    <form @submit.prevent="onSubmit">
      <template v-for="field in entityConfig.fields" :key="field.name">
        <div class="form-group">
          <label :for="field.name">{{ field.label }}</label>

          <!-- Liste de primitifs -->
          <div v-if="(field.list || field.type === 'list') && !field.entity">
            <div v-for="(item, idx) in form[field.name]" :key="idx" class="array-item">
              <input
                :id="field.name + '_' + idx"
                v-model="form[field.name][idx]"
                :type="inputType(field.type === 'list' ? 'text' : field.type)"
                :readonly="field.readonly || false"
                :required="!field.readonly"
              />
              <button type="button" @click="removeFromList(field.name, idx)">❌</button>
            </div>
            <button type="button" class="array-add-btn" @click="addToList(field.name)">Ajouter</button>
          </div>

          <!-- Liste d'entités -->
          <div v-else-if="field.type === 'list' && field.entity">
            <div v-for="(item, idx) in form[field.name]" :key="idx" class="array-item">
              <select
                :id="field.name + '_' + idx"
                v-model="form[field.name][idx]"
                :required="!field.readonly"
              >
                <option disabled value="">-- Sélectionner --</option>
                <option
                  v-for="option in Array.isArray(entityOptions[field.entity]) ? entityOptions[field.entity] : []"
                  :key="option.id"
                  :value="option.id"
                >
                  {{ option.id }}
                </option>
              </select>
              <button type="button" @click="removeFromList(field.name, idx)">❌</button>
            </div>
            <button type="button" class="array-add-btn" @click="addToList(field.name)">Ajouter</button>
          </div>

          <!-- Map -->
          <div v-else-if="field.type === 'map'">
            <div v-for="(pair, idx) in form[field.name]" :key="idx" class="array-item">
              <template v-if="field.type1 && isEntityType(field.type1)">
                <select
                  :id="field.name + '_key_' + idx"
                  v-model="form[field.name][idx].key"
                  :required="!field.readonly"
                >
                  <option disabled value="">-- Clé --</option>
                  <option
                    v-for="option in Array.isArray(entityOptions[field.type1]) ? entityOptions[field.type1] : []"
                    :key="option.id"
                    :value="option.id"
                  >
                    {{ option.id }}
                  </option>
                </select>
              </template>
              <template v-else>
                <input
                  :id="field.name + '_key_' + idx"
                  v-model="form[field.name][idx].key"
                  :type="inputType(field.type1)"
                  :placeholder="'Clé'"
                  :readonly="field.readonly || false"
                  :required="!field.readonly"
                  style="width: 90px"
                />
              </template>
              <template v-if="field.type2 && isEntityType(field.type2)">
                <select
                  :id="field.name + '_val_' + idx"
                  v-model="form[field.name][idx].value"
                  :required="!field.readonly"
                >
                  <option disabled value="">-- Valeur --</option>
                  <option
                    v-for="option in Array.isArray(entityOptions[field.type2]) ? entityOptions[field.type2] : []"
                    :key="option.id"
                    :value="option.id"
                  >
                    {{ option.id }}
                  </option>
                </select>
              </template>
              <template v-else>
                <input
                  :id="field.name + '_val_' + idx"
                  v-model="form[field.name][idx].value"
                  :type="inputType(field.type2)"
                  :placeholder="'Valeur'"
                  :readonly="field.readonly || false"
                  :required="!field.readonly"
                  style="width: 90px"
                />
              </template>
              <button type="button" @click="removeFromMap(field.name, idx)">❌</button>
            </div>
            <button type="button" class="array-add-btn" @click="addToMap(field.name)">Ajouter</button>
          </div>

          <!-- Boolean simple -->
          <input
            v-else-if="inputType(field.type) === 'checkbox'"
            :id="field.name"
            type="checkbox"
            v-model="form[field.name]"
            :readonly="field.readonly || false"
            :style="field.readonly ? 'background-color: #eee; color: #888; cursor: not-allowed;' : ''"
          />

          <!-- Champ normal -->
          <input
            v-else
            :id="field.name"
            v-model="form[field.name]"
            :type="inputType(field.type)"
            :step="field.step || null"
            :readonly="field.readonly || false"
            :required="inputType(field.type) === 'checkbox' ? false : !field.readonly"
            :style="field.readonly ? 'background-color: #eee; color: #888; cursor: not-allowed;' : ''"
          />
        </div>
      </template>
      <button type="submit">{{ form.id ? 'Modifier' : 'Ajouter' }}</button>
      <button v-if="form.id" type="button" @click="resetForm">Annuler</button>
    </form>

    <!-- Tableau générique avec tri + filtre -->
    <table>
      <thead>
        <tr>
          <th v-for="field in entityConfig.fields" :key="field.name"
              @click="setSort(field.name)"
              style="cursor:pointer;user-select:none;">
            {{ field.label }}
            <span v-if="sortField === field.name">
              {{ sortOrder === 1 ? '▲' : '▼' }}
            </span>
          </th>
          <th>Actions</th>
        </tr>
        <tr>
          <th v-for="field in entityConfig.fields" :key="field.name + '-filter'">
            <input
              v-model="filters[field.name]"
              type="text"
              class="filter-input"
              :placeholder="'Filtrer...'"
              @keydown.stop
            />
          </th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in filteredAndSortedItems()" :key="item.id">
          <td v-for="field in entityConfig.fields" :key="field.name">
            {{ formatValue(item[field.name], field) }}
          </td>
          <td>
            <button @click="onEdit(item)">✏️</button>
            <button @click="onDelete(item.id)">🗑️</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch } from 'vue'
import axios from 'axios'

const props = defineProps({
  entityConfig: {
    type: Object,
    required: true
  },
  title: {
    type: String,
    default: ''
  }
})

const entityConfig = props.entityConfig
const title = props.title

const items = ref([])
const form = reactive({})
const entityOptions = reactive({})
const filters = reactive({})
const BASIC_TYPES = ['text', 'number', 'date', 'boolean', 'checkbox']

function isEntityType(type) {
  if (!type) return false
  return !BASIC_TYPES.includes(type.toLowerCase())
}
function inputType(type) {
  if (!type) return 'text'
  if (type.toLowerCase() === 'boolean') return 'checkbox'
  if (type.toLowerCase() === 'checkbox') return 'checkbox'
  if (type.toLowerCase() === 'date') return 'date'
  if (
    type.toLowerCase() === 'number' ||
    type.toLowerCase() === 'int' ||
    type.toLowerCase() === 'float' ||
    type.toLowerCase() === 'double'
  ) return 'number'
  return 'text'
}

// ----- Tri par colonne -----
const sortField = ref(null)
const sortOrder = ref(1) // 1=ASC, -1=DESC

function setSort(field) {
  if (sortField.value === field) {
    sortOrder.value *= -1
  } else {
    sortField.value = field
    sortOrder.value = 1
  }
}

// ----------- Initialisation formulaire et filtres -----------
const initForm = () => {
  entityConfig.fields.forEach(f => {
    if (f.type === 'map') {
      form[f.name] = []
    } else if (f.list || f.type === 'list') {
      form[f.name] = []
    } else if (inputType(f.type) === 'checkbox') {
      form[f.name] = false
    } else {
      form[f.name] = f.readonly ? null : ''
    }
  })
}
const resetForm = () => {
  entityConfig.fields.forEach(f => {
    if (f.type === 'map') {
      form[f.name] = []
    } else if (f.list || f.type === 'list') {
      form[f.name] = []
    } else if (inputType(f.type) === 'checkbox') {
      form[f.name] = false
    } else {
      form[f.name] = f.readonly ? null : ''
    }
  })
}

// Initialise les filtres (une fois le composant monté)
const initFilters = () => {
  entityConfig.fields.forEach(f => {
    if (!(f.name in filters)) filters[f.name] = ''
  })
}

// ----------- Gestion listes dynamiques -----------
function addToList(fieldName) {
  form[fieldName].push('')
}
function removeFromList(fieldName, idx) {
  form[fieldName].splice(idx, 1)
}
function addToMap(fieldName) {
  form[fieldName].push({ key: '', value: '' })
}
function removeFromMap(fieldName, idx) {
  form[fieldName].splice(idx, 1)
}

// ----------- Chargement des options pour les entités -----------
const fetchEntityOptions = async () => {
  const typesToFetch = new Set()
  entityConfig.fields.forEach(f => {
    if ((f.type === 'list' && f.entity)) {
      typesToFetch.add(f.entity)
    }
    if (f.type === 'map') {
      if (isEntityType(f.type1)) typesToFetch.add(f.type1)
      if (isEntityType(f.type2)) typesToFetch.add(f.type2)
    }
  })
  await Promise.all(
    Array.from(typesToFetch).map(async (typeName) => {
      try {
        const url = `http://localhost:8080/api/v1/${typeName.toLowerCase()}s/`
        const res = await axios.get(url)
        if (Array.isArray(res.data)) {
          entityOptions[typeName] = res.data
        } else if (Array.isArray(res.data.results)) {
          entityOptions[typeName] = res.data.results
        } else {
          entityOptions[typeName] = []
        }
      } catch (e) {
        entityOptions[typeName] = []
      }
    })
  )
}

// ----------- Formatage pour affichage tableau -----------
const formatValue = (value, field) => {
  if (field.type === 'map' && Array.isArray(value)) {
    return value.map(pair => (pair.key ?? '') + '→' + (pair.value ?? '')).join(' | ')
  }
  if ((field.list || field.type === 'list') && Array.isArray(value)) {
    return value.join(', ')
  }
  if (field.type === 'boolean') {
    return value ? '✔️' : ''
  }
  if (field.type === 'number' && typeof value === 'number') {
    return value
  }
  return value
}

// ----------- Filtrage + Tri -----------

function filteredAndSortedItems() {
  let filtered = items.value.filter(item => {
    return entityConfig.fields.every(f => {
      const val = formatValue(item[f.name], f)
      const filterText = (filters[f.name] || '').toString().toLowerCase()
      if (!filterText) return true
      return (val !== undefined && val !== null && val.toString().toLowerCase().includes(filterText))
    })
  })

  if (!sortField.value) return filtered
  const fieldDef = entityConfig.fields.find(f => f.name === sortField.value)
  return [...filtered].sort((a, b) => {
    let av = a[sortField.value]
    let bv = b[sortField.value]
    if (fieldDef && (fieldDef.type === 'number' || fieldDef.type === 'int')) {
      av = Number(av)
      bv = Number(bv)
    }
    if (fieldDef && fieldDef.type === 'boolean') {
      av = av ? 1 : 0
      bv = bv ? 1 : 0
    }
    if (fieldDef && fieldDef.type === 'date') {
      av = av || ""
      bv = bv || ""
    }
    if (av == null) av = ""
    if (bv == null) bv = ""
    if (av < bv) return -1 * sortOrder.value
    if (av > bv) return 1 * sortOrder.value
    return 0
  })
}

// ----------- CRUD actions -----------
const fetchItems = async () => {
  try {
    const res = await axios.get(entityConfig.endpoint)
    items.value = res.data
  } catch (err) {
    console.error('Erreur fetchItems:', err)
  }
}

const onSubmit = async () => {
  const payload = {}
  entityConfig.fields.forEach(f => {
    if (!f.readonly) {
      if (f.type === 'map') {
        payload[f.name] = form[f.name]
      } else if ((f.list || f.type === 'list')) {
        payload[f.name] = form[f.name]
      } else if (inputType(f.type) === 'checkbox') {
        payload[f.name] = !!form[f.name]
      } else if (f.type === 'number') {
        const val = form[f.name]
        payload[f.name] = val === '' || val === null ? null : parseFloat(val)
      } else {
        payload[f.name] = form[f.name]
      }
    }
  })

  try {
    if (form.id) {
      await axios.put(`${entityConfig.endpoint}${form.id}/`, {
        ...payload,
        id: form.id
      })
    } else {
      await axios.post(entityConfig.endpoint, payload)
    }
    resetForm()
    await fetchItems()
  } catch (err) {
    console.error('Erreur onSubmit:', err)
  }
}

const onEdit = (item) => {
  entityConfig.fields.forEach(f => {
    if (f.type === 'map') {
      form[f.name] = Array.isArray(item[f.name]) ? JSON.parse(JSON.stringify(item[f.name])) : []
    } else if (f.list || f.type === 'list') {
      form[f.name] = Array.isArray(item[f.name]) ? [...item[f.name]] : []
    } else if (inputType(f.type) === 'checkbox') {
      form[f.name] = !!item[f.name]
    } else {
      form[f.name] = item[f.name]
    }
  })
}

const onDelete = async (id) => {
  try {
    await axios.delete(`${entityConfig.endpoint}${id}/`)
    await fetchItems()
  } catch (err) {
    console.error('Erreur onDelete:', err)
  }
}

let intervalId = null

onMounted(async () => {
  initForm()
  initFilters()
  await fetchItems()
  await fetchEntityOptions()
})

onUnmounted(() => {
  if (intervalId) clearInterval(intervalId)
})
</script>

<style scoped>
.array-item {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}
.array-add-btn {
  background-color: #2ecc71;
  margin-top: 3px;
  margin-bottom: 3px;
}
.array-add-btn:hover {
  background-color: #219150;
}

.generic-crud-container {
  max-width: 900px;
  margin: 40px auto;
  padding: 20px;
  background-color: #fafafa;
  border-radius: 8px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

h1 {
  margin-bottom: 24px;
  font-size: 1.8rem;
  color: #333;
  text-align: center;
}

form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 32px;
}

.form-group {
  flex: 1 1 200px;
  display: flex;
  flex-direction: column;
}

form input,
form textarea {
  padding: 10px 12px;
  font-size: 1rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

form input:focus,
form textarea:focus {
  border-color: #3498db;
  box-shadow: 0 0 3px rgba(52, 152, 219, 0.4);
  outline: none;
}

form button {
  padding: 10px 20px;
  background-color: #3498db;
  color: #fff;
  font-size: 1rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s, transform 0.1s;
}

form button:hover {
  background-color: #2980b9;
}

form button:active {
  transform: scale(0.98);
}

.filter-input {
  width: 100%;
  padding: 3px 5px;
  font-size: 0.95rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-sizing: border-box;
}

.array-item {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}

.array-add-btn {
  background-color: #2ecc71;
  margin-top: 3px;
  margin-bottom: 3px;
}
.array-add-btn:hover {
  background-color: #219150;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.95rem;
  background-color: #fff;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

thead {
  background-color: #f2f2f2;
}

th,
td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

th {
  font-weight: 600;
  color: #555;
}

th:hover {
  background: #dbeafe;
}

tbody tr:hover {
  background-color: #f9f9f9;
}

td button {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.1rem;
  margin-right: 8px;
  transition: opacity 0.2s;
}

td button:last-child {
  margin-right: 0;
}

td button:hover {
  opacity: 0.7;
}

td button:first-child {
  color: #27ae60;
}

td button:last-child {
  color: #e74c3c;
}

@media (max-width: 600px) {
  form {
    flex-direction: column;
  }

  form input,
  form button,
  form textarea {
    width: 100%;
  }

  table th,
  table td {
    padding: 8px 12px;
    font-size: 0.9rem;
  }
}
</style>
