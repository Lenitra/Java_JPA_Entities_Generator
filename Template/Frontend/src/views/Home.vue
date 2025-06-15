<template>
  <div class="home-page-container">
    <h1>Bienvenue sur votre Application CRUD !</h1>

    <section class="riddle">
      <h2>🧩 Énigme 🧩</h2>
      <p>
        Je suis une balise qui aime cacher des secrets,<br>
        Invisible pour l'utilisateur mais précieuse pour le dev.<br>
        Je contiens scripts, styles, et bien des métadonnées.<br>
        Qui suis-je ?
      </p>
    </section>

    <footer class="author">
      <p>Auteur : <strong>Lemartinel Thomas</strong></p>
    </footer>
  </div>
</template>

<script setup>
// Aucun script particulier pour cette page simple
</script>

<style scoped>
.home-page-container {
  max-width: 900px;
  margin: 60px auto;
  padding: 40px;
  background-color: #fafafa;
  border-radius: 8px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
  text-align: center;
}

h1 {
  font-size: 2rem;
  color: #333;
  margin-bottom: 20px;
}

.riddle {
  background-color: #ffffff;
  padding: 20px;
  border-radius: 6px;
  border: 1px solid #e0e0e0;
  margin-top: 30px;
}

.riddle h2 {
  font-size: 1.5rem;
  color: #2980b9;
  margin-bottom: 15px;
}

.riddle p {
  font-size: 1.1rem;
  line-height: 1.6;
}

.author {
  margin-top: 40px;
  color: #555;
  font-size: 0.95rem;
}
</style>
