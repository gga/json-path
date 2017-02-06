## [Unreleased]
### Added
- `query` method to retrieve key path alongside matches (#4).
- Correct filtering for non-nil elements (e.g. `$.books[?(@.isbn)]`).

### Changed
- `$.key` will not match elements in `[{:key "foo"}, {:key "bar"}]` anymore. The correct query for this would be `$[*].key` (#8).
