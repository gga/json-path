## 1.0.0
### Added
- Filtering supported on map structures
- Filtering for non-nil values (e.g. `$.books[?(@.isbn)]`)
- `query` method to retrieve key path alongside matches (#4)

### Changed
- `$.key` will not match elements in `[{:key "foo"}, {:key "bar"}]` anymore. The correct query for this would be `$[*].key` (#8).
