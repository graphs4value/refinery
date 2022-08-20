interface WindowControlsOverlayGeometryChangeEvent extends Event {
  titlebarAreaRect: DOMRect;

  visible: boolean;
}

interface WindowControlsOverlay {
  readonly visible: boolean;

  getTitlebarAreaRect(): DOMRect;

  addEventListener(
    type: 'geometrychange',
    listener: (
      this: WindowControlsOverlay,
      event: WindowControlsOverlayGeometryChangeEvent,
    ) => unknown,
    options?: boolean | AddEventListenerOptions,
  );

  removeEventListener(
    type: 'geometrychange',
    listener: (
      this: WindowControlsOverlay,
      event: WindowControlsOverlayGeometryChangeEvent,
    ) => unknown,
  );
}

interface Navigator {
  windowControlsOverlay?: WindowControlsOverlay;
}
