## 2.1.0
### Added
- Support for nested array selection (https://github.com/gga/json-path/pull/14)
- Support for numbers in filters (e.g. `$[?(@.key>42)]`)

## 2.0.0
### Added
- Support for negative array indexing (e.g. `-1`)

### Changed
- Correctly handle array values for wildcards and recursive descend (https://github.com/gga/json-path/issues/13)

## 1.0.1
### Added
- Support for namespaced keywords (#11)

## 1.0.0
### Added
- Filtering supported on map structures
- Filtering for non-nil values (e.g. `$.books[?(@.isbn)]`)
- `query` method to retrieve key path alongside matches (#4)

### Changed
- `$.key` will not match elements in `[{:key "foo"}, {:key "bar"}]` anymore. The correct query for this would be `$[*].key` (#8).
