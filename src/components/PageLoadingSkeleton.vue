<script setup>
import { computed } from 'vue';
import { routeLoadingName } from '../router/index.js';

const view = computed(() => routeLoadingName.value);
</script>

<template>
  <div class="page-loading" aria-label="Cargando página" aria-live="polite" aria-busy="true">
    <header class="page-loading-header">
      <span class="skeleton-shape menu-shape"></span>
      <span class="skeleton-shape logo-shape"></span>
      <span class="skeleton-shape header-link-shape"></span>
      <span class="skeleton-shape header-button-shape"></span>
    </header>

    <main class="page-loading-main" :class="`page-loading-${view}`">
      <template v-if="view === 'acceso'">
        <section class="skeleton-panel access-panel">
          <span class="skeleton-shape eyebrow-shape"></span>
          <span class="skeleton-shape title-shape"></span>
          <span class="skeleton-shape line-shape"></span>
          <span class="skeleton-shape field-shape"></span>
          <span class="skeleton-shape field-shape"></span>
          <span class="skeleton-shape button-shape"></span>
        </section>
      </template>

      <template v-else-if="view === 'mascota'">
        <div class="skeleton-two-column">
          <section class="skeleton-column">
            <span class="skeleton-shape title-shape"></span>
            <span class="skeleton-shape detail-image-shape"></span>
            <div class="skeleton-thumbnail-row"><span v-for="item in 5" :key="item" class="skeleton-shape thumbnail-shape"></span></div>
            <span class="skeleton-shape line-shape"></span>
            <span class="skeleton-shape line-shape short-shape"></span>
          </section>
          <section class="skeleton-column">
            <span class="skeleton-shape eyebrow-shape"></span>
            <span class="skeleton-shape subtitle-shape"></span>
            <span v-for="item in 6" :key="item" class="skeleton-shape information-shape"></span>
            <span class="skeleton-shape button-shape"></span>
          </section>
        </div>
      </template>

      <template v-else-if="view === 'solicitud' || view === 'proveedor'">
        <span class="skeleton-shape eyebrow-shape"></span>
        <span class="skeleton-shape title-shape"></span>
        <span class="skeleton-shape line-shape medium-shape"></span>
        <div class="skeleton-form-grid">
          <section v-for="section in 3" :key="section" class="skeleton-panel">
            <span class="skeleton-shape subtitle-shape"></span>
            <span v-for="field in 2" :key="field" class="skeleton-shape field-shape"></span>
          </section>
        </div>
        <span class="skeleton-shape button-shape"></span>
      </template>

      <template v-else-if="view === 'seguimiento'">
        <span class="skeleton-shape eyebrow-shape"></span>
        <span class="skeleton-shape title-shape wide-title-shape"></span>
        <span class="skeleton-shape line-shape medium-shape"></span>
        <div class="skeleton-two-column">
          <section class="skeleton-panel"><span class="skeleton-shape subtitle-shape"></span><span class="skeleton-shape field-shape"></span><span class="skeleton-shape line-shape"></span><span class="skeleton-shape button-shape"></span></section>
          <section class="skeleton-panel"><span class="skeleton-shape subtitle-shape"></span><span v-for="item in 4" :key="item" class="skeleton-shape information-shape"></span></section>
        </div>
      </template>

      <template v-else-if="view === 'explorar'">
        <span class="skeleton-shape eyebrow-shape"></span>
        <span class="skeleton-shape title-shape"></span>
        <div class="skeleton-filter-row"><span v-for="item in 4" :key="item" class="skeleton-shape filter-shape"></span></div>
        <div class="skeleton-card-grid"><section v-for="item in 3" :key="item" class="skeleton-panel"><span class="skeleton-shape card-image-shape"></span><span class="skeleton-shape subtitle-shape"></span><span class="skeleton-shape line-shape"></span><span class="skeleton-shape filter-shape"></span></section></div>
      </template>

      <template v-else>
        <div class="skeleton-two-column home-skeleton-hero">
          <section class="skeleton-column"><span class="skeleton-shape eyebrow-shape"></span><span class="skeleton-shape title-shape"></span><span class="skeleton-shape title-shape medium-shape"></span><span class="skeleton-shape line-shape"></span><span class="skeleton-shape line-shape medium-shape"></span><div class="skeleton-filter-row"><span class="skeleton-shape button-shape"></span><span class="skeleton-shape button-shape"></span></div></section>
          <span class="skeleton-shape hero-image-shape"></span>
        </div>
        <div class="skeleton-card-grid action-skeleton-grid"><section v-for="item in 3" :key="item" class="skeleton-panel"><span class="skeleton-shape eyebrow-shape"></span><span class="skeleton-shape subtitle-shape"></span><span class="skeleton-shape line-shape"></span><span class="skeleton-shape line-shape short-shape"></span><span class="skeleton-shape button-label-shape"></span></section></div>
      </template>
    </main>
  </div>
</template>

<style scoped>
.page-loading { min-height: 100vh; background: #fff; color: transparent; }
.page-loading-header { height: 78px; display:flex; align-items:center; gap:28px; padding:0 max(24px, calc((100vw - 1260px)/2)); border-bottom:1px solid #dcebed; }
.page-loading-main { width:min(1260px, calc(100% - 48px)); margin:0 auto; padding:70px 0 90px; }
.skeleton-shape { position:relative; display:block; overflow:hidden; border-radius:8px; background:#ededed; }
.skeleton-shape::after { position:absolute; inset:0; content:""; transform:translateX(-100%); background:linear-gradient(105deg, transparent 25%, rgba(255,255,255,.82) 48%, transparent 70%); animation:loading-shimmer 1.45s ease-in-out infinite; }
.menu-shape { width:42px; height:42px; border-radius:12px; border:1px solid #e2eded; background:#f2f2f2; }
.logo-shape { width:104px; height:34px; margin-right:auto; }
.header-link-shape { width:110px; height:14px; }
.header-button-shape { width:110px; height:44px; border-radius:12px; }
.eyebrow-shape { width:130px; height:11px; margin-bottom:18px; }
.title-shape { width:min(580px, 78%); height:44px; margin-bottom:12px; border-radius:7px; }
.wide-title-shape { width:min(780px, 90%); }
.subtitle-shape { width:66%; height:23px; margin-bottom:16px; }
.line-shape { width:90%; height:13px; margin:9px 0; border-radius:6px; }
.medium-shape { width:68%; }
.short-shape { width:48%; }
.button-shape { width:160px; height:46px; margin:16px 10px 0 0; border-radius:12px; }
.button-label-shape { width:116px; height:12px; margin-top:26px; }
.field-shape { width:100%; height:48px; margin:16px 0; border-radius:9px; }
.information-shape { width:100%; height:76px; margin:8px 0; border-radius:12px; }
.skeleton-two-column { display:grid; grid-template-columns:minmax(0, 1.1fr) minmax(280px, .9fr); gap:28px; }
.skeleton-column { min-width:0; }
.skeleton-panel { min-width:0; padding:24px; border:1px solid #e5e5e5; border-radius:18px; background:#fff; box-shadow:0 8px 24px rgba(20,40,50,.04); }
.detail-image-shape { width:100%; aspect-ratio:1.14; border-radius:22px; }
.skeleton-thumbnail-row { display:grid; grid-template-columns:repeat(5,minmax(0,1fr)); gap:8px; margin:10px 0 22px; }
.thumbnail-shape { height:68px; border-radius:9px; }
.hero-image-shape { width:100%; aspect-ratio:1.2; border-radius:28px; }
.skeleton-form-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:18px; margin:28px 0; }
.skeleton-filter-row { display:flex; flex-wrap:wrap; gap:10px; margin:24px 0; }
.filter-shape { width:112px; height:36px; border-radius:10px; }
.skeleton-card-grid { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:16px; margin-top:24px; }
.card-image-shape { width:100%; aspect-ratio:1.5; margin-bottom:20px; border-radius:12px; }
.access-panel { max-width:460px; margin:0 auto; padding:36px; }
.access-panel .title-shape { width:72%; }
.home-skeleton-hero { align-items:center; gap:56px; }
.action-skeleton-grid { margin-top:70px; }
.page-loading-proveedor .skeleton-form-grid { grid-template-columns:1fr; max-width:900px; }
@keyframes loading-shimmer { to { transform:translateX(100%); } }
@media (max-width:760px) {
  .page-loading-header { height:70px; gap:14px; padding:0 18px; }
  .header-link-shape { display:none; }
  .header-button-shape { width:84px; height:40px; }
  .page-loading-main { width:calc(100% - 36px); padding:46px 0 64px; }
  .skeleton-two-column { grid-template-columns:1fr; gap:22px; }
  .skeleton-form-grid,.skeleton-card-grid { grid-template-columns:1fr; }
  .detail-image-shape { aspect-ratio:1.2; }
  .thumbnail-shape { height:50px; }
  .hero-image-shape { aspect-ratio:1.35; }
  .home-skeleton-hero { gap:28px; }
  .title-shape { height:34px; }
  .action-skeleton-grid { margin-top:40px; }
}
@media (prefers-reduced-motion: reduce) { .skeleton-shape::after { animation-duration:.01ms; } }
</style>
