<script setup>
defineProps({
  title: { type: String, required: true },
  text: { type: String, required: true },
  ctaLabel: { type: String, required: true },
  ctaTo: { type: String, default: '' },
  ctaHref: { type: String, default: '' }
});
</script>
<template>
  <section class="p2-dark-band" :aria-label="title">
    <div class="p2-dark-band-copy"><h2>{{ title }}</h2><p>{{ text }}</p></div>
    <RouterLink v-if="ctaTo" class="p2-dark-band-cta" :to="ctaTo">{{ ctaLabel }}</RouterLink>
    <a v-else class="p2-dark-band-cta" :href="ctaHref">{{ ctaLabel }}</a>
  </section>
</template>
<style scoped>
.p2-dark-band{display:flex;align-items:center;justify-content:space-between;gap:24px;margin:48px 0 0;padding:34px clamp(24px,4vw,44px);border-radius: var(--radius-lg);background:var(--primary-dark);box-shadow:var(--shadow-sm);color:var(--white)}
.p2-dark-band h2{margin:0 0 8px;color:var(--white);font:800 clamp(1.4rem,2.6vw,1.9rem)/1.15 "Poppins",sans-serif;letter-spacing:-.03em}
.p2-dark-band p{margin:0;max-width:620px;color:var(--border);font-size:.9rem;line-height:1.6}
.p2-dark-band-cta{display:inline-flex;flex:0 0 auto;align-items:center;justify-content:center;min-height:48px;padding:12px 22px;border-radius: var(--radius-sm);background:var(--white);color:var(--primary-strong);box-shadow:var(--shadow-sm);font-weight:800;text-decoration:none;white-space:nowrap}
.p2-dark-band-cta:hover{transform:translateY(-2px)}
@media(max-width:700px){.p2-dark-band{flex-direction:column;align-items:flex-start}.p2-dark-band-cta{width:100%;white-space:normal;text-align:center}}
</style>
