<script setup>
import { nextTick, onMounted, ref, watch } from 'vue';

const props = defineProps({
  src: { type: String, default: '' },
  alt: { type: String, default: '' },
  frameClass: { type: [String, Array, Object], default: '' },
  imageClass: { type: [String, Array, Object], default: '' },
  frameStyle: { type: [String, Array, Object], default: undefined },
  imageStyle: { type: [String, Array, Object], default: undefined },
  role: { type: String, default: undefined }
});

const image = ref(null);
const settled = ref(!props.src);

function settleImage() {
  settled.value = true;
}

watch(() => props.src, () => {
  settled.value = !props.src;
  if (!props.src) return;
  nextTick(() => {
    if (image.value?.complete) settleImage();
  });
});

onMounted(() => {
  if (!props.src || image.value?.complete) settleImage();
});
</script>

<template>
  <div
    class="image-loading-frame"
    :class="frameClass"
    :style="frameStyle"
    :role="role"
    :aria-label="role === 'img' ? alt : undefined"
    :aria-busy="!settled"
  >
    <div v-if="!settled" class="image-loading-skeleton" aria-hidden="true"></div>
    <img
      v-if="src"
      ref="image"
      class="image-loading-content"
      :class="imageClass"
      :style="imageStyle"
      :src="src"
      :alt="role === 'img' ? '' : alt"
      @load="settleImage"
      @error="settleImage"
    >
    <slot />
  </div>
</template>
