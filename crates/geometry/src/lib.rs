//! Exact signed-integer geometry primitives and a deterministic baseline spatial index.
//!
//! Authoritative physical coordinates use signed 64-bit database units. This M1 index is
//! intentionally simple and auditable: entries are ordered by minimum X and filtered by
//! exact rectangle intersection. Later acceleration may replace the implementation without
//! changing these public semantics or accepted tests.

use std::time::Instant;

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct Rect {
    pub min_x: i64,
    pub min_y: i64,
    pub max_x: i64,
    pub max_y: i64,
}

impl Rect {
    pub fn new(min_x: i64, min_y: i64, max_x: i64, max_y: i64) -> Result<Self, String> {
        if min_x > max_x || min_y > max_y {
            return Err(format!(
                "invalid rectangle [{min_x},{min_y}]..[{max_x},{max_y}]"
            ));
        }
        Ok(Self {
            min_x,
            min_y,
            max_x,
            max_y,
        })
    }

    pub fn intersects(self, other: Self) -> bool {
        self.min_x <= other.max_x
            && self.max_x >= other.min_x
            && self.min_y <= other.max_y
            && self.max_y >= other.min_y
    }

    pub fn translated(self, delta_x: i64, delta_y: i64) -> Result<Self, String> {
        Ok(Self {
            min_x: self
                .min_x
                .checked_add(delta_x)
                .ok_or_else(|| "rectangle X translation overflow".to_string())?,
            min_y: self
                .min_y
                .checked_add(delta_y)
                .ok_or_else(|| "rectangle Y translation overflow".to_string())?,
            max_x: self
                .max_x
                .checked_add(delta_x)
                .ok_or_else(|| "rectangle X translation overflow".to_string())?,
            max_y: self
                .max_y
                .checked_add(delta_y)
                .ok_or_else(|| "rectangle Y translation overflow".to_string())?,
        })
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Shape {
    pub id: u128,
    pub bounds: Rect,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SpatialIndex {
    shapes: Vec<Shape>,
    by_min_x: Vec<usize>,
}

impl SpatialIndex {
    pub fn build(shapes: Vec<Shape>) -> Self {
        let mut by_min_x: Vec<usize> = (0..shapes.len()).collect();
        by_min_x.sort_unstable_by_key(|index| {
            let shape = shapes[*index];
            (shape.bounds.min_x, shape.bounds.min_y, shape.id)
        });
        Self { shapes, by_min_x }
    }

    pub fn len(&self) -> usize {
        self.shapes.len()
    }

    pub fn is_empty(&self) -> bool {
        self.shapes.is_empty()
    }

    pub fn query(&self, area: Rect) -> Vec<Shape> {
        let upper = self.by_min_x.partition_point(|index| {
            self.shapes[*index].bounds.min_x <= area.max_x
        });
        let mut matches: Vec<Shape> = self.by_min_x[..upper]
            .iter()
            .map(|index| self.shapes[*index])
            .filter(|shape| shape.bounds.intersects(area))
            .collect();
        matches.sort_unstable_by_key(|shape| shape.id);
        matches
    }

    pub fn all_shapes(&self) -> &[Shape] {
        &self.shapes
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct BaselineResult {
    pub shape_count: usize,
    pub match_count: usize,
    pub build_millis: u128,
    pub query_micros: u128,
}

pub fn million_shape_baseline() -> Result<BaselineResult, String> {
    const SHAPE_COUNT: usize = 1_000_000;
    let mut shapes = Vec::with_capacity(SHAPE_COUNT);
    for index in 0..SHAPE_COUNT {
        let x = i64::try_from(index)
            .map_err(|error| format!("shape coordinate conversion failed: {error}"))?
            .checked_mul(4)
            .ok_or_else(|| "shape coordinate overflow".to_string())?;
        shapes.push(Shape {
            id: index as u128,
            bounds: Rect::new(x, 0, x + 2, 2)?,
        });
    }

    let build_started = Instant::now();
    let index = SpatialIndex::build(shapes);
    let build_millis = build_started.elapsed().as_millis();
    let query = Rect::new(400, 0, 438, 2)?;
    let query_started = Instant::now();
    let matches = index.query(query);
    let query_micros = query_started.elapsed().as_micros();
    if matches.len() != 10 {
        return Err(format!(
            "million-shape baseline returned {} matches, expected 10",
            matches.len()
        ));
    }
    Ok(BaselineResult {
        shape_count: index.len(),
        match_count: matches.len(),
        build_millis,
        query_micros,
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn exact_intersection_and_translation_are_overflow_checked() {
        let original = Rect::new(-10, -5, 10, 5).expect("rectangle");
        let translated = original.translated(20, 30).expect("translation");
        assert_eq!(translated, Rect::new(10, 25, 30, 35).expect("expected"));
        assert!(original.intersects(Rect::new(10, 5, 20, 10).expect("touching")));
        assert!(!original.intersects(Rect::new(11, 6, 20, 10).expect("separate")));
        assert!(Rect::new(i64::MAX, 0, i64::MAX, 1)
            .expect("maximum")
            .translated(1, 0)
            .is_err());
    }

    #[test]
    fn spatial_index_returns_stable_id_order() {
        let shapes = vec![
            Shape {
                id: 9,
                bounds: Rect::new(20, 20, 30, 30).expect("shape"),
            },
            Shape {
                id: 2,
                bounds: Rect::new(0, 0, 10, 10).expect("shape"),
            },
            Shape {
                id: 7,
                bounds: Rect::new(5, 5, 15, 15).expect("shape"),
            },
        ];
        let index = SpatialIndex::build(shapes);
        let matches = index.query(Rect::new(8, 8, 25, 25).expect("query"));
        assert_eq!(matches.iter().map(|shape| shape.id).collect::<Vec<_>>(), [2, 7, 9]);
    }

    #[test]
    #[ignore = "explicit M1 million-shape performance baseline"]
    fn indexes_and_queries_one_million_simple_shapes() {
        let result = million_shape_baseline().expect("million-shape baseline");
        assert_eq!(result.shape_count, 1_000_000);
        assert_eq!(result.match_count, 10);
        assert!(result.build_millis < 30_000, "build took {result:?}");
        assert!(result.query_micros < 1_000_000, "query took {result:?}");
    }
}
