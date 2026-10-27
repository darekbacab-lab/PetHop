<template>
  <footer class="site-footer"><div class="footer-container">
    <div class="footer-brand"><RouterLink to="/" class="brand" aria-label="PetHop"><img src="/img/image1.png" alt="PetHop"></RouterLink><p>Plataforma digital para facilitar procesos de adopción responsable.</p></div>
    <div class="footer-column"><h2>Explorar</h2><RouterLink to="/explorar.html">Mascotas</RouterLink><RouterLink to="/#como-funciona">Cómo funciona</RouterLink><RouterLink to="/seguimiento.html">Seguimiento</RouterLink></div>
    <div class="footer-column"><h2>Organizaciones</h2><RouterLink to="/#registrar">Registrar refugio</RouterLink><RouterLink to="/acceso.html">Iniciar sesión</RouterLink></div>
  </div><div class="footer-bottom"><span>© {{ new Date().getFullYear() }} PetHop</span><span>Adopción responsable</span></div></footer>
</template>
