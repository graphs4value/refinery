/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { styled } from '@mui/material/styles';
import { observer } from 'mobx-react-lite';

import { RelationMetadata } from '../xtext/xtextServiceResults';

const ErrorPredicateName = styled('span', {
  name: 'RelationName-Error',
})(({ theme }) => ({
  color: theme.palette.error.main,
}));

const Qualifier = styled('span', {
  name: 'RelationName-Qualifier',
})(({ theme }) => ({
  color: theme.palette.text.secondary,
}));

const FormattedName = observer(function FormattedName({
  name,
  metadata,
}: {
  name: string;
  metadata: RelationMetadata;
}): React.ReactNode {
  const { detail } = metadata;
  if (detail.type === 'class' && detail.abstractClass) {
    return <i>{name}</i>;
  }
  if (detail.type === 'reference' && detail.containment) {
    return <b>{name}</b>;
  }
  if (detail.type === 'predicate' && detail.error) {
    return <ErrorPredicateName>{name}</ErrorPredicateName>;
  }
  return name;
});

function RelationName({
  metadata,
  abbreviate,
}: {
  metadata: RelationMetadata;
  abbreviate?: boolean;
}): JSX.Element {
  const { name, simpleName } = metadata;
  if (abbreviate ?? RelationName.defaultProps.abbreviate) {
    return <FormattedName name={simpleName} metadata={metadata} />;
  }
  if (name.endsWith(simpleName)) {
    return (
      <>
        <Qualifier>
          {name.substring(0, name.length - simpleName.length)}
        </Qualifier>
        <FormattedName name={simpleName} metadata={metadata} />
      </>
    );
  }
  return <FormattedName name={name} metadata={metadata} />;
}

RelationName.defaultProps = {
  abbreviate: false,
};

export default observer(RelationName);
