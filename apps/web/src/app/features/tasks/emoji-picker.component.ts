import { Component, output, CUSTOM_ELEMENTS_SCHEMA, AfterViewInit, ViewChild, ElementRef } from '@angular/core';

@Component({
  selector: 'app-emoji-picker',
  standalone: true,
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<div #pickerContainer></div>`,
  styles: [`:host { display: block; }`]
})
export class EmojiPickerComponent implements AfterViewInit {
  readonly emojiSelected = output<string>();

  @ViewChild('pickerContainer') pickerContainer!: ElementRef<HTMLDivElement>;

  async ngAfterViewInit(): Promise<void> {
    const [{ default: data }, { Picker }] = await Promise.all([
      import('@emoji-mart/data'),
      import('emoji-mart')
    ]);

    // emoji-mart Picker is a custom element — cast through unknown to satisfy TypeScript
    const PickerConstructor = Picker as unknown as new (options: object) => Node;
    const picker = new PickerConstructor({
      data,
      locale: 'fr',
      theme: 'light',
      onEmojiSelect: (emoji: { native: string }) => {
        this.emojiSelected.emit(emoji.native);
      }
    });

    this.pickerContainer.nativeElement.appendChild(picker);
  }
}
