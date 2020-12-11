INSERT INTO Settings (name, value, datatype, position, internal) VALUES ('system/site/organizationWebsite', 'http://my.organization.net', 0, 140, 'n') ON CONFLICT DO NOTHING;
INSERT INTO Settings (name, value, datatype, position, internal) VALUES ('system/internalcontact/aws', 'https://ga-ecat3-contacts.s3-ap-southeast-2.amazonaws.com/ecat_list_sample.csv', 0, 112005, 'n') ON CONFLICT DO NOTHING;
