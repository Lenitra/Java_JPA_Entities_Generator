<template>
  <div>
    <nav>
      <router-link to="/products">Produits</router-link>
      <router-link to="/orders">Commandes</router-link>
    </nav>
    <router-view />
  </div>
</template>


<script setup>
</script>



<style scoped>
/* Conteneur principal */
div {
  max-width: 900px;
  margin: 30px auto;
  padding: 0 20px;
  font-family: Arial, sans-serif;
}

/* Barre de navigation */
nav {
  display: flex;
  gap: 20px;
  margin-bottom: 30px;
  background-color: #fafafa;
  padding: 12px 16px;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

/* Liens de navigation */
nav a {
  text-decoration: none;
  font-size: 1rem;
  color: #555;
  padding: 6px 10px;
  border-radius: 4px;
  transition: background-color 0.2s, color 0.2s;
}

nav a:hover {
  background-color: #e0e0e0;
  color: #333;
}

/* Lien actif (vue-router applique la classe 'router-link-active') */
.router-link-active {
  background-color: #3498db;
  color: #fff !important;
}

/* Zone de contenu chargé par <router-view> */
.router-view {
  background-color: #fff;
  padding: 20px;
  border-radius: 6px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

/* Responsive : navigation en colonne sur petits écrans */
@media (max-width: 600px) {
  nav {
    flex-direction: column;
    gap: 10px;
    padding: 10px;
  }

  nav a {
    width: 100%;
    text-align: center;
  }
}
</style>
