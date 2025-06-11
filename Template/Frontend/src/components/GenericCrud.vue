<template>
  <div class="generic-crud-container">
    <h1>{{ props.title }}</h1>

    <!-- Formulaire générique -->
    <form @submit.prevent="onSubmit">
      <template v-for="field in props.entityConfig.fields" :key="field.name">
        <div class="form-group">
            <label :for="field.name">{{ field.label }}</label>
            <input
              :id="field.name"
              v-model="form[field.name]"
              :type="field.type"
              :step="field.step || null"
              :readonly="field.readonly || false"
              :required="!field.readonly"
              :style="field.readonly ? 'background-color: #eee; color: #888; cursor: not-allowed;' : ''"
            />
        </div>
      </template>

      <button type="submit">
        {{ form.id ? 'Modifier' : 'Ajouter' }}
      </button>
      <button v-if="form.id" type="button" @click="resetForm">
        Annuler
      </button>
    </form>

    <!-- Tableau générique -->
    <table>
      <thead>
      <tr>
      <th v-for="field in props.entityConfig.fields" :key="field.name">
        {{ field.label }}
      </th>
      <th>Actions</th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="item in items" :key="item.id">
      <td v-for="field in props.entityConfig.fields" :key="field.name">
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
import {ref, reactive, onMounted, onUnmounted} from 'vue'
import axios from 'axios'

// On récupère les props sous forme d'un objet "props"
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

const items = ref([])
const form = reactive({})

// Initialise les clefs du formulaire selon props.entityConfig.fields
const initForm = () => {
  props.entityConfig.fields.forEach(f => {
    form[f.name] = f.readonly ? null : ''
  })
}

const resetForm = () => {
  props.entityConfig.fields.forEach(f => {
    form[f.name] = f.readonly ? null : ''
  })
}

const formatValue = (value, field) => {
  if (field.type === 'number' && typeof value === 'number') {
    return value
  }
  return value
}

const fetchItems = async () => {
  try {
    const res = await axios.get(props.entityConfig.endpoint)
    items.value = res.data
  } catch (err) {
    console.error('Erreur fetchItems:', err)
  }
}

const onSubmit = async () => {
  const payload = {}
  props.entityConfig.fields.forEach(f => {
    if (!f.readonly) {
      if (f.type === 'number') {
        const val = form[f.name]
        payload[f.name] = val === '' || val === null ? null : parseFloat(val)
      } else {
        payload[f.name] = form[f.name]
      }
    }
  })

  try {
    if (form.id) {
      await axios.put(`${props.entityConfig.endpoint}${form.id}/`, {
        ...payload,
        id: form.id
      })
    } else {
      await axios.post(props.entityConfig.endpoint, payload)
    }
    resetForm()
    await fetchItems()
  } catch (err) {
    console.error('Erreur onSubmit:', err)
  }
}

const onEdit = (item) => {
  props.entityConfig.fields.forEach(f => {
    form[f.name] = item[f.name]
  })
}

const onDelete = async (id) => {
  try {
    await axios.delete(`${props.entityConfig.endpoint}${id}/`)
    await fetchItems()
  } catch (err) {
    console.error('Erreur onDelete:', err)
  }
}

// Animation aléatoire des lignes
let intervalId = null


onMounted(async () => {
  initForm()
  await fetchItems()
})

onUnmounted(() => {
  if (intervalId) clearInterval(intervalId)
})
</script>

<style scoped>
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

