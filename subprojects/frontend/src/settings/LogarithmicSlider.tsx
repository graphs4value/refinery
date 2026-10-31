/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Slider from '@mui/material/Slider';

export interface LogarithmicSliderMark {
  readonly value: number;
  readonly label: React.ReactNode | undefined;
}

interface LogarithmicSliderProps {
  ariaLabelledby: string;
  color?: 'warning' | 'error' | undefined;
  disabled?: boolean;
  describeValue?(this: void, value: number): string;
  formatValue(this: void, value: number): string;
  marks: readonly LogarithmicSliderMark[];
  maximum: number;
  minimum: number;
  onChange(this: void, value: number): void;
  step: number;
  unlimitedValue?: number;
  value: number;
}

const FINITE_SLIDER_MAX = 1_000;
const UNLIMITED_SLIDER_GAP = 100;
const MARK_SNAP_DISTANCE = 12;

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

function getSliderValue(value: number | number[]): number | undefined {
  return typeof value === 'number' ? value : value[0];
}

function snapToNearestMark(value: number, marks: readonly number[]): number {
  let nearestMark = value;
  let nearestDistance = MARK_SNAP_DISTANCE;
  for (const mark of marks) {
    const distance = Math.abs(mark - value);
    if (distance < nearestDistance) {
      nearestMark = mark;
      nearestDistance = distance;
    }
  }
  return nearestMark;
}

export default function LogarithmicSlider({
  ariaLabelledby,
  color,
  describeValue,
  disabled,
  formatValue,
  marks,
  maximum,
  minimum,
  onChange,
  step,
  unlimitedValue,
  value,
}: LogarithmicSliderProps): React.ReactElement {
  const sliderMax =
    unlimitedValue === undefined
      ? FINITE_SLIDER_MAX
      : FINITE_SLIDER_MAX + UNLIMITED_SLIDER_GAP;
  const valueToSliderValue = (value: number): number => {
    if (unlimitedValue !== undefined && value >= unlimitedValue) {
      return sliderMax;
    }
    const clampedValue = clamp(
      Number.isFinite(value) ? value : minimum,
      minimum,
      maximum,
    );
    return Math.round(
      (Math.log(clampedValue / minimum) / Math.log(maximum / minimum)) *
        FINITE_SLIDER_MAX,
    );
  };
  const sliderValueToValue = (sliderValue: number): number => {
    if (!Number.isFinite(sliderValue)) {
      return minimum;
    }
    if (unlimitedValue !== undefined && sliderValue >= sliderMax) {
      return unlimitedValue;
    }
    const position = clamp(sliderValue, 0, FINITE_SLIDER_MAX);
    const unroundedValue =
      minimum *
      Math.exp((position / FINITE_SLIDER_MAX) * Math.log(maximum / minimum));
    return Math.round(unroundedValue / step) * step;
  };
  const sliderMarks = marks.map(({ value: markValue, label }) => ({
    value: valueToSliderValue(markValue),
    label,
  }));
  const sliderMarkValues = sliderMarks.map(({ value: markValue }) => markValue);

  return (
    <Slider
      aria-labelledby={ariaLabelledby}
      min={0}
      max={sliderMax}
      step={1}
      sx={{ width: '100%' }}
      marks={sliderMarks}
      value={valueToSliderValue(value)}
      valueLabelDisplay="off"
      getAriaValueText={(sliderValue) =>
        (describeValue ?? formatValue)(sliderValueToValue(sliderValue))
      }
      onChange={(_event, sliderValue) => {
        const value = getSliderValue(sliderValue);
        if (value === undefined || !Number.isFinite(value)) {
          return;
        }
        onChange(
          sliderValueToValue(snapToNearestMark(value, sliderMarkValues)),
        );
      }}
      disabled={disabled}
      color={color}
    />
  );
}
