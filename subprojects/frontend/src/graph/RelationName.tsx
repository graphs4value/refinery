/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { styled } from '@mui/material/styles';
import { observer } from 'mobx-react-lite';

import typeHashTextColor from '../utils/typeHashTextColor';
import { RelationMetadata } from '../xtext/xtextServiceResults';

const Qualifier = styled('span', {
  name: 'RelationName-Qualifier',
})(({ theme }) => ({
  color: theme.palette.text.secondary,
}));

const FormattedName = styled('span', {
  name: 'RelationName-FormattedName',
  shouldForwardProp: (propName) => propName !== 'detail',
})<{ metadata: RelationMetadata }>(({ theme, metadata: { detail } }) => {
  let color = theme.palette.text.primary;
  let fontStyle = 'normal';
  let fontWeight = theme.typography.fontWeightRegular;
  let textDecoration = 'none';
  switch (detail.type) {
    case 'class':
      if (detail.color !== undefined) {
        color = typeHashTextColor(detail.color, theme);
        fontWeight = theme.typography.fontWeightMedium;
      }
      if (detail.isAbstract) {
        fontStyle = 'italic';
      }
      break;
    case 'reference':
      if (detail.isContainment) {
        fontWeight = theme.typography.fontWeightBold;
      }
      break;
    case 'pred':
      if (detail.kind === 'error') {
        color = theme.palette.text.secondary;
        textDecoration = 'line-through';
      }
      break;
    default:
      // Nothing to change by default.
      break;
  }
  return { color, fontStyle, fontWeight, textDecoration };
});

function RelationName({
  metadata,
  abbreviate,
}: {
  metadata: RelationMetadata;
  abbreviate?: boolean;
}): JSX.Element {
  const { name, simpleName } = metadata;
  if (abbreviate) {
    return <FormattedName metadata={metadata}>{simpleName}</FormattedName>;
  }
  if (name.endsWith(simpleName)) {
    return (
      <>
        <Qualifier>
          {name.substring(0, name.length - simpleName.length)}
        </Qualifier>
        <FormattedName metadata={metadata}>{simpleName}</FormattedName>
      </>
    );
  }
  return <FormattedName metadata={metadata}>{name}</FormattedName>;
}

export default observer(RelationName);
