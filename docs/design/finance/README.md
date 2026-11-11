# Finance refinement

Authority: preserve and expand the existing black, white, line-icon and red-action system. Scope: Today, Summary, Bank, Budget and transaction entry. Calculations, persistence and account security remain owned by the existing implementation.

Direction: a readable daily ledger. Available money leads; the compact chart explains the allocation; flat category and movement rows expose details. Retain category drill-down and overspending states. Use a cool, desaturated data palette distinct from the neutral panels. Reserve dusty rose for overspending; keep red for launcher actions. Use 24 dp margins, 48 dp controls, tabular amounts, visible action labels and consistent section headings. Avoid decorative animation and nested containers.

References inspected: `../concept/png/Finanzas.png`, the existing finance Compose screens, and `../README.md`. The original concept contributes restrained surfaces and movement rows; the current implementation owns capabilities and data.

`direction.png` is a generated direction reference with illustrative data, not a runtime screenshot or an approved pixel-exact specification. Actual dimensions: 852 × 1846. Its narrow amount/chart arrangement must adapt for large text and balances; no sample data is added to production.

Verification: build and existing finance tests; native emulator captures for Today, Summary, Bank, Budget and entry, including empty states, navigation and narrow/large-text layout. Record actual coverage and limitations in `visual-evidence.json`.

## Implementation

- `FinanceUi.kt`: scoped amount typography, section headings, Back navigation, period selection and accessible bottom navigation.
- `FinanceScreen.kt`, `FinanceToday.kt`: available balance first, compact cool-color allocation chart, category drill-down, explicit expense signs, empty state and a reserved area for Register.
- `FinancePeriod.kt`: equal-width period controls and a distinct label for the free-spending balance.
- `BankView.kt`: named add actions, chart legend controls, readable account rows and large balances.
- `FinanceSettings.kt`, `BudgetPlanner.kt`: consistent hierarchy, visible Back controls, readable placeholders and full-width frequency selection.
- `EntrySheet.kt`: native modal sheet, wrapping transaction-type chips and a labeled Save button disabled until the required input is present.
- `DebugFinancePreview.kt` and the debug manifest: a shell-only native fixture with in-memory sample data. It never opens Room or DataStore and is excluded from release builds.

The layout pass preserved `Budget.kt`, `BudgetSections.kt`, `MoneyColors.kt`, `SavingGoal.kt` and `PiggyBank.kt` byte-for-byte. The subsequent palette pass changes only color definitions in `MoneyColors.kt` and `PiggyBank.kt`; calculation logic remains unchanged. Existing local changes were retained. No dependencies, commits or database migrations were added.

## Cool palette refinement

The grayscale layout screenshots remain the baseline. The requested change is limited to data colors; layout, neutral surfaces, typography and red action/navigation accents retain the existing design.

| Role | Color |
| --- | --- |
| Available money / plenty left | Teal `#8FB9B2` |
| Fixed costs / spending in progress | Steel blue `#8FAEC9` |
| Cards / nearly used budget | Lavender `#AAA0C4` |
| Savings | Sage `#A4B7A9` |
| Secondary chart categories | Slate `#8596B1`, mist `#B6C5D1` |
| Over budget / negative balance | Dusty rose `#C58F9B` |

`MoneyColors.kt` owns the shared data colors. Today and Summary keep category hues stable across periods, filters and row order; Budget and monthly income bars share blue fixed costs, hatched sage savings and teal available money. Bank uses lavender debt, mist receivables, blue fixed costs and teal availability. Amounts and labels remain visible independently of color. The shared `StackChart` adds an optional `downColor` argument so Finance can use dusty rose without changing other modules.

Current captures: [Today](cool-today.png), [Summary](cool-summary.png), [Bank](cool-bank.png), [Budget](cool-budget.png). [Narrow Today](cool-narrow-today.png) uses 360 dp width and font scale 1.3. Current source hashes and inspected states are in [visual-evidence.json](visual-evidence.json). Earlier grayscale screenshots and `layout-evidence.json` are historical layout evidence; their source hashes intentionally refer to the earlier revision.

The palette build and all 60 existing unit tests pass (zero failures/errors/skips). Opaque data colors have calculated contrast ratios of 6.14:1 to 10.44:1 against `#141414`; dimmed chart tracks are decorative and retain visible labels/amounts. Five accepted native captures were inspected. `cool-budget-bottom.png` is a diagnostic capture with incomplete static regions, excluded from accepted evidence. Full warning/saving-goal state coverage and physical-device validation remain unverified.

## Earlier layout validation

`gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline --console=plain` passed: 60 tests, zero failures/errors/skips. `git diff --check` passed for the affected scope.

Captures use Android API 37 at 1080 × 2400, density 420, font scale 1.0, and a narrow 945 × 2100 configuration (360 dp wide), font scale 1.3. Controls were exercised with ADB/UI Automator. The entry flow was tested with an empty amount (Save disabled), then $45, keyboard visible, Save, and category drill-down. Fixture changes were kept in memory.

See [Today](today.png), [Summary](summary.png), [Bank](bank.png), [Budget](budget.png), [entry with keyboard](entry-keyboard.png), [large balances](narrow-bank.png), and the [historical layout receipt](layout-evidence.json). The generated reference guided hierarchy and palette; no pixel-exact match is claimed. The native layout preserves real data, existing chart states and Android typography instead of reproducing the illustration's sample values.

Physical Galaxy S24+ validation, TalkBack traversal, release-device performance and exhaustive secondary flows remain outside this emulator pass. The emulator occasionally returned incomplete static regions; captures must be inspected rather than accepting UI Automator or build success as visual proof.
