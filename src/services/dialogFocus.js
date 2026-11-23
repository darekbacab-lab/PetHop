import { onBeforeUnmount, watch } from 'vue';

const FOCUSABLE_SELECTOR = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

function focusableIn(container) {
  return [...container.querySelectorAll(FOCUSABLE_SELECTOR)].filter(el => el.getClientRects().length > 0);
}

// Atrapa el foco dentro de un diálogo mientras está abierto, lo cierra con
// Escape y devuelve el foco al elemento que lo abrió al cerrarse.
export function useFocusTrap(dialogRef, isOpen, onClose) {
  const readOpen = typeof isOpen === 'function' ? isOpen : () => isOpen.value;
  let previousFocus = null;
  let dialogEl = null;

  function onKeyDown(event) {
    if (!dialogEl) return;
    if (event.key === 'Escape') {
      event.stopPropagation();
      if (onClose) onClose();
      return;
    }
    if (event.key !== 'Tab') return;
    const items = focusableIn(dialogEl);
    if (!items.length) { event.preventDefault(); return; }
    const first = items[0];
    const last = items[items.length - 1];
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
  }

  watch(readOpen, open => {
    if (open) {
      previousFocus = document.activeElement;
      requestAnimationFrame(() => {
        if (!readOpen()) return;
        dialogEl = dialogRef.value;
        if (!dialogEl) return;
        const target = focusableIn(dialogEl)[0] || dialogEl;
        target.focus({ preventScroll: true });
        document.addEventListener('keydown', onKeyDown, true);
      });
    } else {
      document.removeEventListener('keydown', onKeyDown, true);
      dialogEl = null;
      if (previousFocus && typeof previousFocus.focus === 'function') previousFocus.focus();
      previousFocus = null;
    }
  });

  onBeforeUnmount(() => document.removeEventListener('keydown', onKeyDown, true));
}
