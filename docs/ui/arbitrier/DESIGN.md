---
name: Arbitrier
colors:
  surface: '#f9f9ff'
  surface-dim: '#cfdaf2'
  surface-bright: '#f9f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f0f3ff'
  surface-container: '#e7eeff'
  surface-container-high: '#dee8ff'
  surface-container-highest: '#d8e3fb'
  on-surface: '#111c2d'
  on-surface-variant: '#43474f'
  inverse-surface: '#263143'
  inverse-on-surface: '#ecf1ff'
  outline: '#737780'
  outline-variant: '#c3c6d1'
  surface-tint: '#3a5f94'
  primary: '#001e40'
  on-primary: '#ffffff'
  primary-container: '#003366'
  on-primary-container: '#799dd6'
  inverse-primary: '#a7c8ff'
  secondary: '#505f76'
  on-secondary: '#ffffff'
  secondary-container: '#d0e1fb'
  on-secondary-container: '#54647a'
  tertiary: '#1b1f20'
  on-tertiary: '#ffffff'
  tertiary-container: '#303436'
  on-tertiary-container: '#999c9e'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d5e3ff'
  primary-fixed-dim: '#a7c8ff'
  on-primary-fixed: '#001b3c'
  on-primary-fixed-variant: '#1f477b'
  secondary-fixed: '#d3e4fe'
  secondary-fixed-dim: '#b7c8e1'
  on-secondary-fixed: '#0b1c30'
  on-secondary-fixed-variant: '#38485d'
  tertiary-fixed: '#e0e3e5'
  tertiary-fixed-dim: '#c4c7c9'
  on-tertiary-fixed: '#191c1e'
  on-tertiary-fixed-variant: '#444749'
  background: '#f9f9ff'
  on-background: '#111c2d'
  surface-variant: '#d8e3fb'
typography:
  headline-xl:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.05em
  data-table:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  container-max: 1440px
  gutter: 24px
  margin-desktop: 40px
  margin-mobile: 16px
  stack-xs: 4px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 24px
  stack-xl: 48px
---

## Brand & Style
The design system is engineered for the high-stakes environment of B2B decision-making and data management. The brand personality is rooted in **trust, authority, and transparency**, ensuring that users feel grounded in a reliable environment when handling complex information.

The visual style follows a **Modern Corporate** aesthetic. It prioritizes clarity and efficiency, utilizing a systematic approach to whitespace and information density. By leaning into a utilitarian yet refined philosophy, the UI removes cognitive friction, allowing business outcomes and data integrity to take center stage. The aesthetic is clean and professional, avoiding decorative trends in favor of a timeless, functional interface that conveys institutional stability.

## Colors
The color strategy is designed to establish a clear hierarchy of information and signal system status with precision.

- **Primary Blue:** Used for core branding, primary actions, and active states to reinforce authority and trust.
- **Slate Gray & Crisp White:** Form the foundation of the interface, providing a clean, low-strain backdrop for long-form data reading.
- **Functional Accents:** Success Green and Alert Amber are reserved strictly for status communication (e.g., "Verified," "Pending Review," "Action Required") to ensure they remain high-signal.
- **Neutral Palette:** A sophisticated range of grays is used for borders, secondary text, and iconography to maintain a balanced, professional contrast ratio.

## Typography
This design system utilizes **Inter** for its exceptional legibility in data-dense SaaS applications. The typeface features a tall x-height and distinct letterforms, which are critical for distinguishing characters in complex alphanumeric strings.

The hierarchy is strictly defined to guide the user through workflows:
- **Headlines:** Use tighter letter-spacing and heavier weights to provide clear section anchoring.
- **Body Text:** Optimized for reading endurance with generous line heights.
- **Data-Specific Styles:** A dedicated "data-table" role is provided for high-density grids, ensuring maximum information visibility without overcrowding the layout.
- **Labels:** Small caps or bold weights are used for form headers and metadata tags to differentiate them from interactive content.

## Layout & Spacing
The layout follows a **Fixed-Fluid Hybrid** model. Large desktop displays use a centered 12-column grid with a 1440px cap to prevent line lengths from becoming unreadable. Tablet and Mobile layouts transition to 8-column and 4-column fluid grids respectively.

A strict **8px spacing scale** governs the vertical rhythm.
- **Data Density:** In table-heavy views, vertical padding may be reduced to `stack-sm` (8px) to increase information density. 
- **Standard Layouts:** Use `stack-md` (16px) or `stack-lg` (24px) for standard component spacing to maintain a sense of "Generous Whitespace" and prevent the UI from feeling cluttered.
- **Section Breaks:** Use `stack-xl` (48px) to clearly separate major logical groups on a page.

## Elevation & Depth
In this design system, depth is used sparingly to maintain a modern, flat SaaS aesthetic. Hierarchy is primarily established through **Tonal Layers** and **Subtle Shadows**.

- **Level 0 (Base):** The primary background color (Crisp White or very light Slate).
- **Level 1 (Cards/Tables):** Surfaces are raised using a 1px border in a light neutral gray (`#E2E8F0`) or an extremely soft ambient shadow (e.g., `0 1px 3px rgba(0,0,0,0.05)`).
- **Level 2 (Overlays/Dropdowns):** Use a more defined shadow with a larger blur radius to indicate temporary interaction layers.
- **Contrast Outlines:** Interactive elements like input fields use a low-contrast 1px outline that strengthens in color upon focus, rather than relying on heavy shadows.

## Shapes
The shape language is **Soft and Professional**. By using a `0.25rem` (4px) base radius, the UI feels modern and approachable without losing the "serious" tone required for B2B enterprise software.

- **Small Components:** Buttons, inputs, and checkboxes use the base `rounded` (4px) setting.
- **Large Components:** Cards and modals use `rounded-lg` (8px) to create a clear structural container.
- **Status Badges:** Use `rounded-xl` (12px) or a full pill-shape to distinguish them from interactive buttons.

## Components
- **Buttons:** Primary buttons are solid "Arbitrier Blue" with white text. Secondary buttons use a ghost style (border only) or a light gray fill to denote lower priority.
- **Data Tables:** These are the heart of the system. They feature sticky headers, zebra-striping on hover, and high-contrast text. Row height is standardized to 48px for comfortable scanning.
- **Status Badges:** Compact, pill-shaped elements using a light tint of the status color (Success, Warning, Error) with dark text of the same hue for maximum accessibility.
- **Input Fields:** Minimalist design with a 1px border. Labels are always persistent (not floating) to ensure the user never loses context during data entry.
- **Steppers:** Linear progress indicators located at the top of multi-step forms, using primary blue for completed steps and slate gray for upcoming ones.
- **Cards:** Used to group related data points. Cards should not have heavy shadows; instead, use a 1px border and a white background against the slightly off-white application base.