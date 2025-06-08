declare namespace AMap {
  class Map {
    constructor(container: HTMLElement, options?: MapOptions);
    addControl(control: any): void;
    add(marker: Marker): void;
  }

  class Marker {
    constructor(options: MarkerOptions);
    getPosition(): LngLat;
    on(event: string, callback: () => void): void;
  }

  class InfoWindow {
    constructor(options: InfoWindowOptions);
    open(map: Map, position: LngLat): void;
  }

  class LngLat {
    constructor(lng: number, lat: number);
  }

  class Pixel {
    constructor(x: number, y: number);
  }

  class Scale {
    constructor();
  }

  class ToolBar {
    constructor();
  }

  interface MapOptions {
    zoom?: number;
    center?: [number, number];
    viewMode?: string;
  }

  interface MarkerOptions {
    position: [number, number];
    title?: string;
  }

  interface InfoWindowOptions {
    content: string;
    offset?: Pixel;
  }
} 